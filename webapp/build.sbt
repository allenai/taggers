import Dependencies._

name := "taggers-webapp"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
  akkaModule("actor"),
  sprayModule("can"),
  sprayModule("routing"),
  sprayJson,
  "com.github.scopt" %% "scopt" % "3.2.0"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

javaOptions += "-Xmx1G"

javaOptions += "-XX:+UseConcMarkSweepGC"

fork in run := true
