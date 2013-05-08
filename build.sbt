organization := "edu.washington.cs.knowitall.taggers"

name := "taggers"

version := "1.0.0-SNAPSHOT"

crossScalaVersions := Seq("2.10.1", "2.9.2")

scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head }

libraryDependencies ++= Seq(
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-core" % "2.4.1",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-chunk-opennlp" % "2.4.1",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-stem-morpha" % "2.4.1",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-typer-stanford" % "2.4.1",
    "edu.washington.cs.knowitall" % "openregex" % "1.0.3",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "jdom" % "jdom" % "1.1",
     "com.google.guava" % "guava" % "14.0.1",
    "junit" % "junit" % "4.11" % "test",
    "org.specs2" %% "specs2" % "1.12.3" % "test")

scalacOptions ++= Seq("-unchecked", "-deprecation")
