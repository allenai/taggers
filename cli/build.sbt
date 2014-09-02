import Dependencies._

name := "taggers-cli"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.2.0") ++ loggingImplementations

scalacOptions ++= Seq("-unchecked", "-deprecation")

// SBT native packager configs.
packageArchetype.java_application
