lazy val root = Project("root", file("."))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    name := "Scala Starter",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.4.1",
      "org.typelevel" %% "cats-effect" % "2.3.1",
      "org.scalatest" %% "scalatest" % "3.2.3" % Test,
    ),
  )

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.13.4",
  scalacOptions --= {
    if (sys.env.get("CI").isDefined) {
      Seq.empty
    } else {
      Seq("-Xfatal-warnings")
    }
  },
  addCompilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full,
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
)
