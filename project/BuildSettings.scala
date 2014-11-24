import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin._

import spray.revolver.RevolverPlugin._

object BuildSettings {
  val defaultBuildSettings = Defaults.defaultSettings ++ releaseSettings ++ Format.settings ++ Revolver.settings ++
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
      // AllenAi Public Resolver
      resolvers += "AllenAI Releases" at "http://utility.allenai.org:8081/nexus/content/repositories/public-releases",
      // Factorie Resolver
      resolvers += "IESL Releases" at "http://dev-iesl.cs.umass.edu/nexus/content/groups/public",
      scalacOptions += "-target:jvm-1.7",
      publishTo <<= version { (v: String) =>
        val nexus = "http://utility.allenai.org:8081/nexus/content/repositories/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "snapshots")
        else
          Some("releases"  at nexus + "releases") }
    )
}
