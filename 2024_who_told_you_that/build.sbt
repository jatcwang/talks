val diffliciousVersion = "0.4.2"

lazy val root = Project("root", file("."))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    name := "Scala Starter",
    libraryDependencies ++= Seq(
      "com.github.jatcwang" %% "difflicious-munit" % diffliciousVersion,
      "com.github.jatcwang" %% "difflicious-cats" % diffliciousVersion,
      "org.typelevel" %% "otel4s-java" % "0.4.0",
    ),
  )

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "3.3.1",
  scalacOptions --= {
    if (sys.env.get("CI").isDefined) {
      Seq.empty
    } else {
      Seq("-Xfatal-warnings")
    }
  },
)
