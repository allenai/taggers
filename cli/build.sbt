name := "taggers-cli"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.2.0",
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.0.4")

scalacOptions ++= Seq("-unchecked", "-deprecation")
