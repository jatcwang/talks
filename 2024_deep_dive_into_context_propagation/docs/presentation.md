<p style="font-size: 4rem; margin: 0">
<span style="">Deep dive into Context Propagation</span>
</p>
<p style="margin: 0">

[//]: # (<span style="font-size: 2rem; color: #504f4f"></span>)
</p>

<div style="font-size: 2rem;">
Jacob Wang
</div>

<span style="font-size: 1.5rem">London Scala User Group, 2024 March</span>

<style>
  .no-box img {
    box-shadow: none!important;
  }
  
  .slides {
    margin-top: 30px!important;
  }
  
  .reveal .slides h3 {
    margin-top: -20px;
    color: brown;
    text-transform: inherit;
  }

  .reveal h1 {
    text-transform: none;
  }

  .reveal h2 {
    text-transform: none;
  }

  .reveal pre code {
    max-height: 800px;
    font-size: 16px;
  }

  .reveal code {
  }

  .diff-render {
      background: aliceblue;
      border-radius: 5px;
      padding: 15px!important;
      white-space: pre;
  }

  .diff-render .red {
     color: red;
  }

  .diff-render .green {
     color: green;
  }

  .diff-render .gray {
     color: gray;
  }
</style>

## This talk

::: nonincremental

- Mechanisms for context propagation
- **otel4s** and **OpenTelemetry**

:::

## Hello

::: nonincremental

- Work
  at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- Maintainer of Doobie and author of Difflicious + other libs
- https://mas.to/@jatcwang

:::

## Context Propagation - Large and Small

<img class="fragment no-box" width="500px" src="assets/images/microservices.svg" />

- Context: Not used for fulfilling the request, but instead for monitoring and debugging
  - e.g. Trace ID, Tenant ID, User ID
- This talk: Passing context _within_ an application
- Invisible / Non-local mechanisms only

# java.lang.ThreadLocal

- Store state **local** to the code executing on that thread only
- ThreadLocal instances are "key" to set/remove their corresponding value stored in the thread
- Conceptually, each Thread has a `Map[ThreadLocal[A], A]`

<div class="fragment">
```scala mdoc:silent
val TL_FOO: ThreadLocal[Int] = ThreadLocal.withInitial(() => 0)
val TL_BAR: ThreadLocal[Int] = ThreadLocal.withInitial(() => 0)

TL_FOO.set(5)

TL_FOO.get() // == 5
TL_BAR.get() // == 0, because TL_BAR is a different "key" from TL_FOO

TL_FOO.remove()

TL_FOO.get() // == 0
```
</div>


---

```scala
val t1 = new Thread(() => {
  TL_FOO.get()   // == 0
  TL_FOO.set(1)
  TL_FOO.get()   // == 1
}, "t1").start()

val t2 = new Thread(() => {
  TL_FOO.get()   // == 0
  TL_FOO.set(3)
  TL_FOO.get()   // == 3
}, "t2").start()
```

# java.lang.ThreadLocal

- Allow us to pass context without explicit parameters
- Java logging **Mapped Diagnostic Context** (MDC) and **OpenTelemetry** pass their context using ThreadLocal
- Can use `InheritableThreadLocal` to pass on context to child threads
  - e.g. background tasks

# The trouble with (many) Threads

- Old-school HTTP libraries create a new thread per request
- Each thread has a base cost of ~1MB
- Threads are often blocked
- CPU cores switching between threads ("context switching") are expensive

# Let's reuse threads! 💡

- **threadpool**: pool of reusable threads and submit "tasks" (`Runnable`) to it
- `Runnable` (equivalent to `() => Unit`) have much smaller overhead
- "Async" 
- Java/Scala Futures and Effect runtimes (Cats-effect, ZIO) are all built on top of threadpools

<div class="fragment">
```scala
trait Runnable { 
  def run(): Unit
}

trait Executor { // The threadpool interface
  def execute(task: Runnable): Unit
}
```
</div>

---

```scala
val threadpool = Executors.newFixedThreadPool(4)
val httpClient = ...
val finishCallback: Result => Unit = ...

threadpool.execute(() => {
  println(s"step 1")
  val httpRequest = ...
  
  httpClient.send(httpRequest, onResponse = response => {
    // Callback will submit next task to threadpool
    threadpool.execute(() => {
      val result = doWork(response)
      finishCallback(result)
    })
    
  })
})
```

---

<img class="no-box" width="600px" src="assets/images/forgot.jpg" />

## Losing Context

```scala
  val threadpool = Executors.newFixedThreadPool(4) // 4 threads in pool
  val CONTEXT = ThreadLocal.withInitial(() => "no user")
  
  threadpool.execute(() => {
    CONTEXT.set("user1")
    println(s"${Thread.currentThread().getName}: step 1 for ${CONTEXT.get()}")
    
    threadpool.execute(() => {
      println(s"${Thread.currentThread().getName}: step 2 for ${CONTEXT.get()}")
      
      threadpool.execute(() => {
        println(s"${Thread.currentThread().getName}: complete for ${CONTEXT.get()}")
      })
      
    })
  })
```

<div class="fragment">
```
pool-1-thread-1: step 1 for user1
pool-1-thread-2: step 2 for no_user
pool-1-thread-3: complete for no_user
```
</div>

- Lost the context because **another thread** executed step 2
- Worse, another unrelated task executing on thread 1 now has "user 1" as its context!

# Making ThreadLocal work with threadpools

- 💡 Capture ThreadLocal value in the submitting thread and set it when running in the new thread

<div class="fragment">
```scala
class CurrentContextExecutor(delegate: Executor) extends Executor:
  override def execute(task: Runnable): Unit =
    val toAttach = CONTEXT.get()                       // T1
    val wrappedTask = wrap(task, toAttach)             // T1
    delegate.execute(wrappedTask)                      // T1
    
  def wrap(task: Runnable, toAttach: Context): Runnable =
    () =>
      try                                              //   T2  
        CONTEXT.set(toAttach)                          //   T2   
        task.run()                                     //   T2
      finally                                          //   T2
        CONTEXT.remove()                               //   T2
```
</div>

- Resurface the context we want to pass on  (Line 3)
- Set the context we want to pass on (Line 10)
- Remove the ThreadLocal value (Line 13)

## cats.effect.IOLocal

- With Cats-Effect, _can_ use `ThreadLocal` + task wrapping trick we just learned
- But we have a nicer solution: `IOLocal`
- `IOLocal` allow us to pass context through the executing fiber
- On fork, child fibers inherit a copy of the parent's context

## cats.effect.IOLocal Example

```scala
for
  // Create the IOLocal "key"
  CONTEXT <- IOLocal(default = 0) 
  
  _ <- CONTEXT.get                   // == 0
  _ <- CONTEXT.set(5)
  
  // fork!
  fiber1 <- (for {       
    _ <- CONTEXT.get                 // == 5
    _ <- CONTEXT.set(6)
    _ <- CONTEXT.get                 // == 6
  } yield ()).start
  
  _ <- CONTEXT.get                   // == 5
  _ <- CONTEXT.set(10)
  _ <- fiber1.joinWithNever
  _ <- CONTEXT.get                   // == 10
yield ()
```

## What we've seen so far

- **ThreadLocal**
- **Thread pools** and how ThreadLocal can work with them
- `IOLocal` for cats-effect
- Now, let's apply what we've learned 

# OpenTelemetry - Quick Intro

- **OpenTelemetry**: An open standard for Distributed Tracing, Metrics, etc
- **Span**: A recorded unit of work
  - Attributes include `traceId`, `parentSpanId`, `isError` and `duration`
- **Trace**: Links together a set of spans (same `traceId`), so we can track the execution trace from start to finish across services.

---

<img class="no-box" width="100%" src="assets/images/trace_view.png" />

--- 

## Otel4s - OpenTelemetry for Scala

- Typelevel ecosystem
- Follows OTel terminology and specification, but does not directly use OpenTelemetry-Java types
- Uses OpenTelemetry-Java as backend (e.g. reporting spans)

## OpenTelemetry - Instrumentation API concepts

- `Context`: Immutable key-value pairs for storing all OTel-related objects
  - The `Context` object itself is propagated around (e.g. ThreadLocal, IOLocal)
  - e.g. `Span` object is stored in `Context`
  - ```
      val context = Map(
        SPAN_KEY -> Span(traceId, spanId, ...),
        BAGGAGE_KEY -> Baggage(..)
      )
    ```
- `OpenTelemetry`/`Otel4s`: Central service for handling reporting spans and metrics
- `Tracer`: Use to create spans. Created from `OpenTelemetry` instance

## Otel4s - Minimal Example

```scala mdoc:silent
import cats.effect._
import cats.implicits.*
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.trace.Tracer
```

```scala mdoc:silent
for
  otel4s <- OtelJava.global[IO] // Initialize otel-java backend of Otel4s
  given Tracer[IO] <- otel4s.tracerProvider.get("my_app") 

  _ <- Tracer[IO].span("root").surround(      // Start a span, surrounding an IO
    for
      _ <- Tracer[IO].span("child_1").surround(
        IO.println("work work")
      )
      _ <- Tracer[IO].span("child_2").surround(
        Tracer[IO].span("grandchild_1").surround(
          IO.println("more work done")
        )
      )
    yield ()
  )
yield ()
```

---

<img class="no-box" width="95%" src="assets/images/trace_simple.png" />

# Integrating with Java libraries

- In Otel4s, we use IOLocal to propagate context, 
  but Java libs use opentelemetry-java (`ThreadLocal`)
- 💡 Extract `Context` from IOLocal and set it up as ThreadLocal before calling the java code

<div class="fragment">
```scala mdoc:silent
import cats.mtl.Local
import io.opentelemetry.context.Context as JContext // Context type from Java OTel

def blockingWithContext[A](use: => A)(using local: Local[IO, Context]): IO[A] =
  for
    context <- local.ask
    result <- IO.blocking {
      val jContext: JContext = context.underlying
      val scope = jContext.makeCurrent()       // Set the ThreadLocal
      try
        use
      finally
        scope.close()                          // Unset the ThreadLocal
    }
  yield result
```
</div>

# Integrating with Java libraries - Example

- From cats-effect, use Java 11's `HttpClient` to call another service
- **Expectations**: Downstream service continues the trace

```scala mdoc:invisible
import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import io.opentelemetry.api.GlobalOpenTelemetry

class MyService(javaHttpClient: HttpClient) {
  val _ = javaHttpClient
  def doWorkAndMakeRequest: IO[Unit] = IO.unit
}

```

<div class="fragment">
```scala mdoc:silent
import io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry

for
  otel4s <- OtelJava.global[IO]
  given Tracer[IO] <- otel4s.tracerProvider.get("my_app")
  given Local[IO, Context] = otel4s.localContext

  httpClient = HttpClient.newBuilder().build()
  instrumentedHttpClient = JavaHttpClientTelemetry
          .builder(GlobalOpenTelemetry.get())
          .build()
          .newHttpClient(httpClient)
  myService = MyService(instrumentedHttpClient)
  
  _ <- myService.doWorkAndMakeRequest
yield ()
```
</div>

# Integrating with Java libraries - Example



```scala mdoc:silent:nest
class MyService(javaHttpClient: HttpClient)(using Local[IO, Context], Tracer[IO]):
  
  def doWorkAndMakeRequest: IO[Unit] =
    withSpan("root")(for 
      _ <- withSpan("child_1")(IO.println("work work"))
      
      _ <- withSpan("child_2")(for
        req <- IO(HttpRequest.newBuilder()
          .uri(new URI("http://localhost:8080/example"))
          .GET()
          .build())
        
        resp <- blockingWithContext {
            javaHttpClient.send(req, BodyHandlers.ofString)
          }
        _ <- IO.println(resp.body())
      yield ())
      
    yield ())
  
  def withSpan[F[_], A](name: String)(using tracer: Tracer[F])(io: F[A]): F[A] =
    tracer.span(name).surround(io)
```

---

<img class="no-box" width="95%" src="assets/images/trace_complex.png" />

# Final thoughts

- Otel4s supports metrics too!
- use OTel-java's `Context.taskWrapping` to wrap threadpools
  - Many async libraries allow you to pass in your own threadpool (`Executor`)
- Prefer Manual instrumentation? (Instead of using OTel Java agent)

# Thanks to..

- otel4s and OpenTelemetry
- https://github.com/keuhdall/otel4s-grafana-example
- Scala 3
