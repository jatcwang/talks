package example

import cats.effect.*
import cats.implicits.*
import cats.mtl.Local
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context as JContext
import io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.trace.Tracer

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}

object Trace2Main extends IOApp.Simple {
  
  val run: IO[Unit] =
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
}

extension [F[_], A](fa: F[A])
  def markSpan(name: String)(using T: Tracer[F]): F[A] = {
    T.span(name).surround(fa)
  }

def withSpan[F[_], A](name: String)(using T: Tracer[F])(io: F[A]): F[A] =
  T.span(name).surround(io)

def blockingWithContext[A](use: => A)(using L: Local[IO, Context]): IO[A] =
  L.ask.flatMap{ ctx =>
    IO.blocking {
      val jContext: JContext = ctx.underlying
      val scope = jContext.makeCurrent()
      try {
        use
      } finally {
        scope.close()
      }
    }
  }

class MyService(javaHttpClient: HttpClient)(using Local[IO, Context], Tracer[IO]) {

  def doWorkAndMakeRequest: IO[Unit] =
    withSpan("root")(for 
      _ <- withSpan("child_1")(IO.println("work work"))
      
      _ <- withSpan("child_2")(for
        req <- IO(HttpRequest.newBuilder()
          .uri(new URI("http://localhost:8080/example"))
          .GET()
          .build())
        
        _ <- withSpan("grandchild_1")(blockingWithContext {
          javaHttpClient.send(req, BodyHandlers.ofString)
        }.flatMap(resp => IO.println(resp.body())))
        
        _ <- withSpan("grandchild_2")(IO.println("more work"))
      yield ())
      
    yield ())
}
