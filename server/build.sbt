import Dependencies._

name := "taggers-server"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
  akkaModule("actor"),
  sprayModule("can"),
  sprayModule("routing"),
  "com.github.scopt" %% "scopt" % "3.2.0"
)

dependencyOverrides ++= Set(
  "com.github.scopt" %% "scopt" % "3.2.0"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")
