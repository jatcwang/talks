val root = Project("root", file("."))
  .enablePlugins(TutPlugin)
  .settings(
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % "0.11.1"
    )
  )
