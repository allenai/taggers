organization := "edu.washington.cs.knowitall.taggers"

name := "taggers"

version := "0.1"

crossScalaVersions := Seq("2.10.1", "2.9.2")

scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head }

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
    "com.google.guava" % "guava" % "12.0",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-core" % "2.4.2",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-chunk-opennlp" % "2.4.2",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-stem-morpha" % "2.4.2",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-typer-stanford" % "2.4.2",
    "edu.washington.cs.knowitall" % "openregex" % "1.0.3",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "org.jdom" % "jdom2" % "2.0.5",
    "junit" % "junit" % "4.11" % "test",
    "org.specs2" %% "specs2" % "1.12.3" % "test")

scalacOptions ++= Seq("-unchecked", "-deprecation")

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
