name := "taggers-webapp"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "net.databinder" %% "unfiltered-filter" % "0.6.8",
    "net.databinder" %% "unfiltered-jetty" % "0.6.8")

scalacOptions ++= Seq("-unchecked", "-deprecation")
