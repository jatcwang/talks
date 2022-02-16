val diffliciousVersion = "0.4.2-SNAPSHOT"

lazy val root = Project("root", file("."))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    name := "Scala Starter",
    libraryDependencies ++= Seq(
      "com.github.jatcwang" %% "difflicious-munit" % diffliciousVersion,
      "com.github.jatcwang" %% "difflicious-cats" % diffliciousVersion,
    ),
  )

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "3.1.1",
  scalacOptions --= {
    if (sys.env.get("CI").isDefined) {
      Seq.empty
    } else {
      Seq("-Xfatal-warnings")
    }
  },
)
