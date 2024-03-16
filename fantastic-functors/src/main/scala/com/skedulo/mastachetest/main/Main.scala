package com.skedulo.mastachetest.main

import scalaz._, Scalaz._
import io.circe._

object Main extends App {

  Option(5).map(_.toString) == Option("5")

  List(1,2,3).map(_.toString) == List("1", "2", "3")

  \/.right[Double, Int](3).map(_.toString) == \/.right[Double, String]("3")

  Json.fromString("Bolto")

}
