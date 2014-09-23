addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-release" % "2014.09.11-0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

// Native packager, for deploys.
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

// Check for updates.
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.4")
