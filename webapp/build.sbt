name := "taggers-webapp"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.1.0",
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.0.4",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion)

scalacOptions ++= Seq("-unchecked", "-deprecation")
