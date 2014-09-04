import Dependencies._

ReleaseSettings.defaults

name := "taggers-core"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    nlpstackModule("core"),
    nlpstackModule("chunk"),
    nlpstackModule("lemmatize"),
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.1.2",
    "org.apache.commons" % "commons-lang3" % "3.3",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    slf4j)
