name := "taggers-webapp"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.1.0",
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.0.4",
    "net.databinder" %% "unfiltered-filter" % "0.6.8",
    "net.databinder" %% "unfiltered-jetty" % "0.6.8")

scalacOptions ++= Seq("-unchecked", "-deprecation")
