import Dependencies._

name := "taggers-cli"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.2.0"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")
