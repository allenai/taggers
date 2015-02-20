import Dependencies._
import sbtrelease.ReleasePlugin._

releaseSettings

name := "taggers-core"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    commonPipeline,
    nlpstackModule("core"),
    nlpstackModule("chunk"),
    nlpstackModule("lemmatize"),
    "org.allenai.openregex" %% "openregex-scala" % "1.1.3",
    "org.apache.commons" % "commons-lang3" % "3.3",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" %% "scalatest" % "2.2.2" % "test",
    slf4j)
