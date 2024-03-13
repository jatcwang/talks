val otelVersion = "1.34.1"
val otel4sVersion = "0.5.0-RC1"

lazy val root = Project("root", file("."))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    name := "Scala Starter",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "otel4s-oteljava" % otel4sVersion,
      "org.typelevel" %% "otel4s-core" % otel4sVersion,
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % otelVersion % Runtime,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % otelVersion % Runtime,
      "io.opentelemetry" % "opentelemetry-api" % otelVersion,
      "io.opentelemetry.instrumentation" % "opentelemetry-java-http-client" % "2.1.0-alpha"
    ),
    Compile / javaOptions ++= Seq(
      "-Dotel.java.global-autoconfigure.enabled=true",
      s"-Dotel.service.name=my_app"
    ),
    fork := true
  )

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "3.4.0",
  scalacOptions --= {
    if (sys.env.get("CI").isDefined) {
      Seq.empty
    } else {
      Seq("-Xfatal-warnings")
    }
  },
)
