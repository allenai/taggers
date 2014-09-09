import sbt.Keys._
import sbt._

import spray.revolver.RevolverPlugin._

object BuildSettings {
  val defaultBuildSettings = Defaults.defaultSettings ++ ReleaseSettings.defaults ++ Format.settings ++ Revolver.settings ++
    Seq(
      organization := "org.allenai.taggers",
      crossScalaVersions := Seq("2.10.4"),
      scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
      scalacOptions ++= Seq("-target:jvm-1.7", "-unchecked", "-deprecation"),
      javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
      homepage := Some(url("http://github.com/knowitall/taggers")),
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      conflictManager := ConflictManager.strict,
      dependencyOverrides := Dependencies.defaultDependencyOverrides,
      resolvers += "AllenAI Snapshots" at "http://utility.allenai.org:8081/nexus/content/repositories/snapshots",
      resolvers += "AllenAI Releases" at "http://utility.allenai.org:8081/nexus/content/repositories/releases",
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      resolvers += "IESL Releases" at "http://dev-iesl.cs.umass.edu/nexus/content/groups/public",
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
}
