val thisScalaVersion = "2.12.8"

val root = Project("root", file("."))
  .enablePlugins(MdocPlugin)
  .settings(
    scalaVersion := thisScalaVersion,
    scalacOptions ++= Seq("-language:higherKinds", "-Ypartial-unification"),

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.0.0",
      "com.softwaremill.quicklens" %% "quicklens" % "1.4.12",
      "io.scalaland" %% "chimney" % "0.3.2",
      "com.softwaremill.diffx" %% "diffx-scalatest" % "0.3.2",

      "org.scalatest" %% "scalatest" % "3.0.7" % "test",
    )
  )
