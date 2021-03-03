package example

import cats.effect.{ContextShift, IO, Timer}

import java.util.concurrent.{Executors, TimeUnit}
import cats.effect.laws.util.TestContext

import scala.concurrent.duration._

object Main {

  def main(args: Array[String]): Unit = {
    val testCtx = TestContext()
    implicit val ctxShift: ContextShift[IO] = testCtx.ioContextShift
    implicit val timer: Timer[IO] = testCtx.ioTimer

    val io = IO {
      println("IO is running!")
    }.delayBy(100.days)

    ctxShift.evalOn(testCtx)(io).unsafeRunAsyncAndForget()

    println("Ticking 50 days..")
    testCtx.tick(50.days)
    println("Ticking another 50 days..")
    testCtx.tick(50.days)
  }

}
