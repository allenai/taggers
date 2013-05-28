organization := "edu.washington.cs.knowitall.taggers"

name := "taggers"

description := "Tag sentences with XML-specified logic."

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

licenses := Seq("Academic License" -> url("http://reverb.cs.washington.edu/LICENSE.txt"))

homepage := Some(url("http://github.com/knowitall/taggers"))

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

publishMavenStyle := true

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

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
  </developers>)
