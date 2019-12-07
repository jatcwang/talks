package com.example

object Example {

  sealed trait AllErrors extends Throwable
  final case class E1() extends AllErrors
  final case class E2() extends AllErrors
  final case class E3() extends AllErrors

}
