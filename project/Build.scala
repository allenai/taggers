import sbt._
import Keys._

object TaggerBuild extends Build {
  override lazy val settings = super.settings ++
    Seq(
      organization := "edu.washington.cs.knowitall.taggers",
      version := "0.2",
      crossScalaVersions := Seq("2.10.1", "2.9.3"),
      scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
      scalacOptions ++= Seq("-unchecked", "-deprecation"),
      licenses := Seq("Academic License" -> url("http://reverb.cs.washington.edu/LICENSE.txt")),
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
    id = "tagger-core",
    base = file("core"),
    settings = Project.defaultSettings ++
      Seq())

  lazy val webapp = Project(
    id = "tagger-webapp",
    base = file("webapp"),
    settings = Project.defaultSettings ++
      Seq()) dependsOn(core)
}
