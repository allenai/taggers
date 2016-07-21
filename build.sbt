lazy val buildSettings = Seq(
  organization := "org.allenai.taggers",
  crossScalaVersions := Seq(Dependencies.defaultScalaVersion),
  scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/allenai/taggers")),
  apiURL := Some(url("https://allenai.github.io/taggers/")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/allenai/taggers"),
    "https://github.com/allenai/taggers.git")),
  pomExtra := (
    <developers>
      <developer>
        <id>allenai-dev-role</id>
        <name>Allen Institute for Artificial Intelligence</name>
        <email>dev-role@allenai.org</email>
      </developer>
    </developers>),
  bintrayPackage := s"${organization.value}:${name.value}_${scalaBinaryVersion.value}"
)

lazy val core = Project(id = "core", base = file("core"))
  .settings(buildSettings)
  .enablePlugins(LibraryPlugin)

lazy val cli = Project(id = "cli", base = file("cli"))
  .settings(buildSettings)
  .dependsOn(core)

lazy val server = Project(id = "server", base = file("server"))
  .settings(buildSettings)
  .dependsOn(core)

lazy val webapp = Project(id = "webapp", base = file("webapp"))
  .settings(buildSettings)
  .dependsOn(core)

lazy val root = Project(id = "taggers", base = file("."))
  .settings(
    publish := {},
    publishTo := Some("dummy" at "nowhere"),
    publishLocal := {})
  .aggregate(core, cli, server, webapp)
