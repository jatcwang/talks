package com.example

import cats.implicits._
import cats.syntax.show._

object App {
  import UrlShow._

  def main(args: Array[String]): Unit = {
    import cats.effect._

    val x = s"sdf"
    println(urlshow"$x")
  }

  trait UrlShow[A] {
    def urlShow(a: A): String
  }

  object UrlShow {

    implicit val stringUrlShow: UrlShow[String] = str => str

    final case class UrlShowInterpolator(_sc: StringContext) extends AnyVal {
      def urlshow(args: UrlShown*): String = _sc.s(args: _*)
    }

    final case class UrlShown(override val toString: String) extends AnyVal

    object UrlShown {
      implicit def mat[A](x: A)(implicit z: UrlShow[A]): UrlShown = UrlShown(z.urlShow(x))
    }

    implicit final def urlShowInterpolator(sc: StringContext): UrlShowInterpolator = UrlShowInterpolator(sc)

  }

}
