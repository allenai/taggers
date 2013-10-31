import sbt._
import Keys._

object TaggerBuild extends Build {
  val nlptoolsVersion = SettingKey[String]("nlptools-version", "The version of nlptools used for building.")

  lazy val root = Project(id = "taggers", base = file(".")).settings (
    publish := { },
    publishTo := Some("bogus" at "http://nowhere.com"),
    publishLocal := { }
  ).aggregate(core, webapp)

  val buildSettings = Defaults.defaultSettings ++ ReleaseSettings.defaults ++
    Seq(
      organization := "edu.washington.cs.knowitall.taggers",
      crossScalaVersions := Seq("2.10.2"),
      scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
      nlptoolsVersion := "2.4.4-SNAPSHOT",
      scalacOptions ++= Seq("-unchecked", "-deprecation"),
      homepage := Some(url("http://github.com/knowitall/taggers")),
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
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
    id = "taggers-core",
    base = file("core"),
    settings = buildSettings)

  lazy val webapp = Project(
    id = "taggers-webapp",
    base = file("webapp"),
    settings = buildSettings) dependsOn(core)
}
