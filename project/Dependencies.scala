import sbt.Keys._
import sbt._

object Dependencies {
  val defaultDependencyOverrides = Set(
      "com.typesafe" % "config" % "1.2.1",
      "org.slf4j" % "log4j-over-slf4j" % "1.7.10",
      "org.scala-lang" % "scala-library" % "2.11.5",
      "org.scala-lang" % "scala-reflect" % "2.11.5",
      "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2",
      "com.google.guava" % "guava" % "15.0",
      "org.slf4j" % "slf4j-api" % "1.7.6",
      "org.parboiled" %% "parboiled-scala" % "1.1.6",
      "com.github.scopt" %% "scopt" % "3.2.0",
      "commons-codec" % "commons-codec" % "1.6")

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.6"
  val logbackVersion = "1.1.2"
  val logbackCore = "ch.qos.logback" % "logback-core" % logbackVersion
  val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
  val loggingImplementations = Seq(logbackCore, logbackClassic)

  val sprayJson = "io.spray" %% "spray-json" % "1.3.1"

  val commonPipeline = "org.allenai.common" %% "common-pipeline" % "2015.01.22-0"

  def nlpstackModule(id: String) = "org.allenai.nlpstack" %% s"nlpstack-$id" % "1.2"
  def akkaModule(id: String) = "com.typesafe.akka" %% s"akka-$id" % "2.3.2"
  def sprayModule(id: String) = "io.spray" %% s"spray-$id" % "1.3.1"
}
