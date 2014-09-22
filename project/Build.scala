import org.allenai.sbt.release.AllenaiReleasePlugin

import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

import BuildSettings._

object TaggerBuild extends Build {
  lazy val root = Project(id = "taggers-root", base = file(".")).settings (
    publish := { },
    publishTo := Some("bogus" at "http://nowhere.com"),
    publishLocal := { }
  ).aggregate(core, cli, server, webapp)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = defaultBuildSettings).enablePlugins(AllenaiReleasePlugin)

  lazy val webapp = Project(
    id = "webapp",
    base = file("webapp"),
    settings = defaultBuildSettings) dependsOn(core)

  lazy val cli = Project(
    id = "cli",
    base = file("cli"),
    settings = defaultBuildSettings) dependsOn(core)

  lazy val server = Project(
    id = "server",
    base = file("server"),
    settings = defaultBuildSettings) dependsOn(core)
}
