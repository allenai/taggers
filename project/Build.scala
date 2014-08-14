import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object TaggerBuild extends Build {
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.6"
  val logbackVersion = "1.1.1"
  val logbackCore = "ch.qos.logback" % "logback-core" % logbackVersion
  val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
  val loggingImplementations = Seq(logbackCore, logbackClassic)

  val sprayVersion = "1.3.1"
  val akkaVersion = "2.3.2"
  val nlpstackVersion = "0.8"

  lazy val root = Project(id = "taggers-root", base = file(".")).settings (
    publish := { },
    publishTo := Some("bogus" at "http://nowhere.com"),
    publishLocal := { }
  ).aggregate(core, cli, server, webapp)

  val buildSettings = Defaults.defaultSettings ++ ReleaseSettings.defaults ++ Format.settings ++ Revolver.settings ++
    Seq(
      organization := "org.allenai.taggers",
      crossScalaVersions := Seq("2.10.4"),
      scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
      scalacOptions ++= Seq("-target:jvm-1.7", "-unchecked", "-deprecation"),
      javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
      homepage := Some(url("http://github.com/knowitall/taggers")),
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      conflictManager := ConflictManager.strict,
      dependencyOverrides ++= Set(
          "org.scala-lang" % "scala-reflect" % "2.10.3",
          "com.google.guava" % "guava" % "15.0",
          "org.scala-lang" % "scala-library" % "2.10.4",
          "org.slf4j" % "slf4j-api" % "1.7.6",
          "org.parboiled" %% "parboiled-scala" % "1.1.6",
          "com.github.scopt" %% "scopt" % "3.2.0"),
      resolvers += "AllenAI Snapshots" at "http://utility.allenai.org:8081/nexus/content/repositories/snapshots",
      resolvers += "AllenAI Releases" at "http://utility.allenai.org:8081/nexus/content/repositories/releases",
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      scalacOptions += "-target:jvm-1.7",
      publishMavenStyle := true,
      publishTo <<= version { (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2") },
      pomExtra := (
        <scm>
          <url>https://github.com/knowitall/taggers</url>
          <connection>scm:git://github.com/knowitall/taggers.git</connection>
          <developerConnection>scm:git:git@github.com:knowitall/taggers.git</developerConnection>
          <tag>HEAD</tag>
        </scm>
        <developers>
         <developer>
            <name>Michael Schmitz</name>
          </developer>
        </developers>))

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = buildSettings)

  lazy val webapp = Project(
    id = "webapp",
    base = file("webapp"),
    settings = buildSettings) dependsOn(core)

  lazy val cli = Project(
    id = "cli",
    base = file("cli"),
    settings = buildSettings) dependsOn(core)

  lazy val server = Project(
    id = "server",
    base = file("server"),
    settings = buildSettings) dependsOn(core)
}
