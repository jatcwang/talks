package example

import cats.effect.*

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

object FiberTestApp extends IOApp.Simple {
  def run: IO[Unit] =
    IOLocal(0).flatMap(CONTEXT => new FiberTest(CONTEXT).run())
}

