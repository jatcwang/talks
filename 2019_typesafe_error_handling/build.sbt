val thisScalaVersion = "2.12.8"

val root = Project("typesafe-error-handling", file("."))
  .enablePlugins(MdocPlugin)
  .settings(
    scalaVersion := thisScalaVersion,
    scalacOptions ++= Seq("-language:higherKinds", "-Ypartial-unification"),

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.0.0",
      "com.github.jatcwang" %% "hotpotato-core" % "0.0.7",
      "com.chuusai" %% "shapeless" % "2.3.3",

      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    )
  )
