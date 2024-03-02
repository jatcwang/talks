<p style="font-size: 4rem; margin: 0">
<span style="">Who told you that?!</span>
</p>
<p style="margin: 0">
<span style="font-size: 2rem; color: #504f4f">Deep dive into Context Propagation</span>
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

## Hello

::: nonincremental

- Software Developer
  at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- https://mas.to/@jatcwang

:::

## Passing Context

- What do we mean by Context?

<img class="fragment no-box" width="500px" src="assets/images/microservices.svg" />

- E.g. Tenant ID, User ID, or anything else relevant for the current request
- May not be necessary to fulfill the request, but useful for debugging when things aren't quite working!

# java.lang.ThreadLocal

```scala mdoc
object Example:
  val MY_TL = ThreadLocal.withInitial(() => 1)

  def printTL() = println(s"${Thread.currentThread().getName()}: ${MY_TL.get()}")

  def set2(): Unit =
    printTL()
    MY_TL.set(2)

  def set3(): Unit =
    printTL()
    MY_TL.set(3)
```

---

```scala mdoc:silent
// Let's run it
val t1 = new Thread(() => {
  Example.set2()
  Example.set3()
  Example.printTL()
}, "t1").start()

val t2 = new Thread(() => {
  Example.set2()
  Example.set3()
  Example.printTL()
}, "t2").start()

// Output:
// t2: 1
// t1: 1
// t2: 2
// t2: 3
// t1: 2
// t1: 3
```

# ThreadLocals

- Allow us to pass context without explicit parameters
- Java's Mapped Diagnostic Context (MDC) and OpenTelemetry pass their context using ThreadLocal
- Can use `InheritableThreadLocal` to pass on context to child threads
  - e.g. background tasks

# The trouble with Threads

- Older HTTP libraries create a new thread per request
- But each thread has a base cost of ~1MB
- CPU Core switching between threads ("context switching") is expensive
- Not _web-scale_

# Let's reuse threads! 💡

- Have a pool of threads and submit "tasks" (Runnable) to it
- `Runnable` (equivalent to `() => Unit`) have much smaller overhead
- "Async"

```scala
trait Runnable { 
  def run(): Unit
}

trait Executor { // The threadpool interface
  def execute(task: Runnable): Unit
}
```

---

```scala
val threadpool = Executors.newFixedThreadPool(4)

threadpool.execute(() => {
    println(s"step 1")
    val x = "a"
    
    threadpool.execute(() => {
        println(s"step 2")
        val y = x ++ "b"
        
        threadpool.execute(() => {
            println(s"complete! $y")
        })
    })
})
```

- Java/Scala Futures, Effect runtimes (Cats-effect, ZIO) are all built on this

---

<img class="no-box" width="600px" src="assets/images/forgot.jpg" />

---

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

- We've lost the context because another thread executed step 2
- Worse, another unrelated task executing on thread 1 now has "user 1" as its context!

# Making ThreadLocal work with Async

- 💡 Capture ThreadLocal value in the submitting thread and set it when running in the new thread

```scala
class CurrentContextExecutor(delegate: Executor) extends Executor:
  override def execute(task: Runnable): Unit =
    val toAttach = CONTEXT.get()                /*1*/  // T1
    val wrappedTask = wrap(task, toAttach)             // T1
    delegate.execute(wrappedTask)                      // T1
    
  def wrap(task: Runnable, toAttach: Context): Runnable =
    () => {                                  
      val beforeAttach = CONTEXT.get()          /*2*/  //   T2
      try                                              //   T2  
        CONTEXT.set(toAttach)                   /*3*/  //   T2   
        task.run()                                     //   T2
      finally                                          //   T2
        CONTEXT.set(beforeAttach)               /*4*/  //   T2
    }
```

1. Resurface the context we want to pass on
2. In the new thread, save the context to restore too 
3. Set the context we want to pass on
4. Restore the previous context on T2

## cats.effect.IOLocal

::: nonincremental

- With Cats-Effect, we *can* use `ThreadLocal` + Wrapping trick we just covered
- But we have a better solution: `IOLocal`
- `IOLocal` allow us to pass context through our fiber
- Like `InheritableThreadLocal`, child fibers inherit a copy of the parent's context when forked
- Not based on `ThreadLocal`, need explicit extraction if calling code that needs it

:::

## cats.effect.IOLocal Example

```scala
class FiberTest(CONTEXT: IOLocal[Int]) {
  def run(): IO[Unit] =
    for {
      _ <- CONTEXT.set(5)
      fiber1 <- (for {
        _ <- CONTEXT.get                 // = 5
        _ <- CONTEXT.set(6)
        _ <- CONTEXT.get                 // = 6
      } yield ()).start
      _ <- CONTEXT.get                   // = 5
      _ <- CONTEXT.set(10)
      _ <- fiber1.joinWithNever
      _ <- CONTEXT.get                   // = 10
    } yield ()
}
```

## What we've seen so far

- `ThreadLocal`: Context passing for synchronous code
- **Thread pools**: Need to extract and pass the context stored in ThreadLocal to the next executing thread
- In Cats-Effect, we can use `IOLocal` (`FiberRef` in ZIO) to pass context
- Now, let's apply what we've learned today

# OpenTelemetry - Quick Intro

- **Distributed Tracing**: A method of tracking application requests as they flow in a distributed system
- **OpenTelemetry**: An open standard for telemetry (Distributed Tracing, Metrics, etc)
- **Span**: A recorded unit of work
  - Attributes include `parentSpanId`, `traceId`, `isError` and `duration`
- **Trace**: Links together a set of spans (same `traceId`), so we can track the execution trace from start to finish across services.

---

<img class="no-box" width="100%" src="assets/images/trace_view.png" />

--- 

## OpenTelemetry - Instrumentation API concepts

- **Context**: The OpenTelemetry context. Key-value pairs for storing all OT-related objects
  - The `Context` object itself is propagated around (e.g. ThreadLocal, IOLocal)
  - `Span` object is stored in `Context`
  - ```
      val context = Map(
        SPAN_KEY -> Span(traceId, spanId, ...),
        BAGGAGE_KEY -> Baggage(..)
      )
    ```
- **Tracer**: Use to create spans

## FIXME

FIXME: Specify https://github.com/typelevel/cats-effect/pull/3636 somewhere
FIXME: io.opentelemetry.context.Context.taskWrapping to wrap an ExecutorService to propagate across async boundaries


# The (Near) Future

- With Virtual Threads (Java 21+) and ScopedValues 

# THE END
