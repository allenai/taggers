import sbt._

import org.allenai.plugins.CoreDependencies

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies extends CoreDependencies {
  def nlpstackModule(id: String) = "org.allenai.nlpstack" % s"nlpstack-${id}_2.11" % "1.6"
}
