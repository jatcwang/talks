package com.example

import zio._

object App {

  def main(args: Array[String]): Unit = {
    val v: ZIO[Any, Nothing, String] = IO.die(new Exception("asdf"))

    println("Hello com.example.Empty Project!")
  }

}
