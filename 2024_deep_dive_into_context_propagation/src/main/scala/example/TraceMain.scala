package example

import cats.effect.*
import cats.implicits.*
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.trace.Tracer

object TraceMain extends IOApp.Simple {
  val run: IO[Unit] = {
    for
      otel4s <- OtelJava.global[IO]
      given Tracer[IO] <- otel4s.tracerProvider.get("my_app")

      _ <- Tracer[IO].span("root").surround(
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
  }
}
