ReleaseSettings.defaults

name := "taggers-core"

description := "Tag sentences with XML-specified logic."

libraryDependencies ++= Seq(
    "com.google.guava" % "guava" % "13.0.1",
    "org.allenai.nlptools" %% "nlptools-core" % nlptoolsVersion.value,
    "org.allenai.nlptools" %% "nlptools-chunk-opennlp" % nlptoolsVersion.value,
    "org.allenai.nlptools" %% "nlptools-stem-morpha" % nlptoolsVersion.value,
    "edu.washington.cs.knowitall" %% "openregex-scala" % "1.1.2",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
    "org.specs2" % "specs2" % "1.12.3" % "test" cross CrossVersion.binaryMapped {
      case "2.9.3" => "2.9.2"
      case "2.10.2" => "2.10"
      case x => x
    })
