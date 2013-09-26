name := "taggers-core"

description := "Tag sentences with XML-specified logic."

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
    "com.google.guava" % "guava" % "13.0.1",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-core" % "2.4.2",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-chunk-opennlp" % "2.4.2",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-stem-morpha" % "2.4.2",
    "edu.washington.cs.knowitall.nlptools" %% "nlptools-typer-stanford" % "2.4.2",
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.0.4",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "org.jdom" % "jdom2" % "2.0.5",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
    "org.specs2" % "specs2" % "1.12.3" % "test" cross CrossVersion.binaryMapped {
      case "2.9.3" => "2.9.2"
      case "2.10.1" => "2.10"
      case x => x
    })
