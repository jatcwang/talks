package example

import cats.effect.*
import cats.implicits.*
import cats.mtl.Local
import cats.effect.Sync
import cats.mtl.Local
import cats.syntax.flatMap.*
import io.opentelemetry.context.Context as JContext
import cats.mtl.syntax.local.given
import org.typelevel.otel4s.java.instances.localForIoLocal
import org.typelevel.otel4s.java.context.Context
import org.typelevel.otel4s.java.OtelJava
import org.typelevel.otel4s.trace.Tracer
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span as JSpan
import io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry
import io.opentelemetry.api.trace.SpanContextKey

import java.net.URI
import java.net.http.{HttpClient, HttpRequest}
import java.net.http.HttpResponse.BodyHandlers

object Trace2Main extends IOApp.Simple {

  def go[F[_] : Async : Tracer](javaHttpClient: HttpClient)(using L: Local[F, Context]): F[Unit] = {
    (for {
      _ <- Async[F].delay(println("hey")).markSpan("child_1")
      _ <- (for {
        _ <- {
          val req = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:7777/get"))
            .GET()
            .build()
          blockingWithContext {
            javaHttpClient.send(req, BodyHandlers.ofString)
          }
            .map(resp => println(resp.body()))
        }.markSpan("grandchild_1")
        _ <- Async[F].delay(println("ha")).markSpan("grandchild_2")
      } yield ()).markSpan("child_2")
    } yield ()).markSpan("root", root = true)
  }

  extension [F[_], A](fa: F[A])
    def markSpan(name: String, root: Boolean = false)(using T: Tracer[F]): F[A] = {
      T.span(name).surround(fa)
    }

  def blockingWithContext[F[_] : Sync, A](use: => A)(using L: Local[F, Context]): F[A] =
    Local[F, Context].ask.flatMap { ctx =>
      Sync[F].blocking {
        val jContext: JContext = ctx.underlying
        val scope = jContext.makeCurrent()
        try {
          use
        } finally {
          scope.close()
        }
      }
    }


  val run: IO[Unit] =
    for {
      given IOLocal[Context] <- IOLocal(Context.root)
      otel4s = OtelJava.local[IO](GlobalOpenTelemetry.get())
      given Tracer[IO] <- otel4s.tracerProvider.get("lol")
      given Local[IO, Context] = localForIoLocal[IO, Context]

      javaHttpClient = JavaHttpClientTelemetry.builder(GlobalOpenTelemetry.get()).build().newHttpClient(HttpClient.newBuilder().build())
      _ <- go[IO](javaHttpClient)
    } yield ()
}
