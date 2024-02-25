package example

import cats.effect.{ExitCode, IO, IOApp, IOLocal, FiberIO}

import scala.concurrent.duration.*

object FiberTest extends IOApp {

  def printLocal(fiberName: String, local: IOLocal[Int]): IO[Unit] =
    local.get.flatMap(i => IO.println(s"$fiberName context is $i"))

  def updateLocal(name: String, updateF: Int => Int, CONTEXT: IOLocal[Int]): IO[Unit] =
    (for {
      _ <- printLocal(name, CONTEXT) // print before
      _ <- CONTEXT.update(updateF)
      _ <- printLocal(name, CONTEXT) // print after
    } yield ())

  def run(): IO[Unit] =
    for {
      CONTEXT <- IOLocal(0)
      _ <- printLocal("main", CONTEXT)
      fiber1 <- updateLocal("f1", _ + 1, CONTEXT).start // forked child fiber!
      _ <- fiber1.joinWithNever
      _ <- updateLocal("main", _ => 5, CONTEXT)
      _ <- printLocal("main", CONTEXT)
    } yield ()

  def run(args: List[String]): IO[ExitCode] =
    run().as(ExitCode.Success)

}
