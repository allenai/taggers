name := "taggers-server"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.2.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion)

scalacOptions ++= Seq("-unchecked", "-deprecation")

// SBT native packager configs.
packageArchetype.java_application
