
name := "fantastic-functors"
organization := "com.skedulo"
version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

// Allow ctrl-c to kill the server during development
cancelable in Global := true
fork in Global := true

val enumeratumVersion = "1.4.15"

lazy val resourceTracking = project.in(file("."))
  .settings(credentialSettings: _*)
  .settings(
    addCompilerPlugin("org.scalamacros"  %% "paradise" % "2.1.0" cross CrossVersion.full)
  )

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-feature",
  "-encoding", "UTF8",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xlint",
  "-deprecation",
  "-Xfuture"
)

// Stricter checks
//scalacOptions in (Compile, test) ++= Seq(
//  "-Ywarn-dead-code",
//  "-Ywarn-unused-import",
//  "-Xfatal-warnings"
//)

libraryDependencies ++= Seq(
  // Skedulo libs
  "com.skedulo" %% "skeduloutils" % "0.2.1",

  // Better Enums for Scala
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-circe" % enumeratumVersion,

  // Templating engine
  "org.scalatra.scalate" %% "scalate-core" % "1.7.1",

  // cats
//  "org.typelevel" %% "cats" % "0.8.0",
  "org.scalaz" %% "scalaz-core" % "7.2.6",

  "io.circe" %% "circe-core" % "0.5.4",
  "io.circe" %% "circe-generic" % "0.5.4",

  // Logging libraries
  "ch.qos.logback" % "logback-classic"        % "1.1.3"

)

lazy val credentialSettings = Seq(
  coursierCredentials := Map(
    "nexus-ivy-proxy-releases" -> coursier.Credentials(Path.userHome / ".ivy2" / ".credentials"),
    "nexus-maven-proxy-releases" -> coursier.Credentials(Path.userHome / ".ivy2" / ".credentials")
  )
)


