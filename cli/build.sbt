name := "taggers-cli"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.2.0",
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.0.4") ++ loggingImplementations

scalacOptions ++= Seq("-unchecked", "-deprecation")

// SBT native packager configs.
packageArchetype.java_application
