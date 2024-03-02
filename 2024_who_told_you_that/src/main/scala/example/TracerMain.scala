package example

import cats.effect._
import cats.implicits.*

import cats.mtl.Local

import org.typelevel.otel4s.java.context.Context
import org.typelevel.otel4s.java.OtelJava
import org.typelevel.otel4s.trace.Tracer
import io.opentelemetry.api.GlobalOpenTelemetry

object TraceMain extends IOApp.Simple {
  def program[F[_] : Async : Tracer]: F[Unit] = {
    Tracer[F].span("root").surround(
      for {
        _ <- Tracer[F].span("child_1").surround(
          Async[F].delay(println("hey"))
        )
        _ <- Tracer[F].span("child_2").surround(
          Tracer[F].span("grandchild_1").surround(
            Async[F].delay(println("ha"))
          )
        )
      } yield ()
    )
  }
  
  val run: IO[Unit] =
    for {
      otel4s <- IO(GlobalOpenTelemetry.get).flatMap(OtelJava.forAsync[IO])
      given Tracer[IO] <- otel4s.tracerProvider.get("")
      _ <- program[IO]
    } yield ()
}
