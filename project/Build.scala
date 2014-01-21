import sbt._
import Keys._

object TaggerBuild extends Build {
  val nlptoolsVersion = SettingKey[String]("nlptools-version", "The version of nlptools used for building.")

  lazy val root = Project(id = "taggers", base = file(".")).settings (
    publish := { },
    publishTo := Some("bogus" at "http://nowhere.com"),
    publishLocal := { }
  ).aggregate(core, cli, webapp)

  val buildSettings = Defaults.defaultSettings ++ ReleaseSettings.defaults ++ Format.settings ++
    Seq(
      organization := "org.allenai.taggers",
      crossScalaVersions := Seq("2.10.3"),
      scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
      nlptoolsVersion := "2.4.4",
      scalacOptions ++= Seq("-unchecked", "-deprecation"),
      homepage := Some(url("http://github.com/knowitall/taggers")),
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
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
}
