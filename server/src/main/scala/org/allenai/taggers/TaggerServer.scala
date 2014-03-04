package org.allenai.taggers

import akka.actor._
import spray.http._
import spray.routing._
import spray.routing.Directives._

import java.io.File

object TaggerServerMain extends SimpleRoutingApp {
  case class Config(port: Int = 8080, cascadeFile: File = null)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("taggers") {
      arg[File]("<file>") action { (x, c) =>
        c.copy(cascadeFile = x)
      } text ("file specifying cascade")

      opt[Int]("port") action { (x, c) =>
        c.copy(port = x)
      } text ("port to run on")
    }

    parser.parse(args, Config()) match {
      case Some(config) => run(config)
      case None =>
    }
  }

  def run(config: Config): Unit = {
    val cascade = Cascade.load(config.cascadeFile, config.cascadeFile.getName)
    val app = new ChunkedTaggerApp(cascade)

    val info = Map(
        "name" -> cascade.name
      )

    def infoRoute: Route = get {
      path("info") {
        complete {
          info.keys mkString "\n"
        }
      } ~
      path("info" / Segment) { key =>
        complete {
          info.get(key) match {
            case Some(key) => key
            case None => (StatusCodes.NotFound, "Could not find info: " + key)
          }
        }
      }
    }

    implicit val system = ActorSystem("tagger-server")

    startServer(interface = "0.0.0.0", port = config.port) {
      path("") {
        get {
          complete {
            s"POST a sentence to extract it with ${cascade.name}."
          }
        } ~
        // TODO(schmmd): this should return a Future, but OpenNLP is not threadsafe.
        // The models (the bulk of the memory footprint) ARE threadsafe.  Ideally
        // there would be a blocking queue of 8 to 16 OpenNLP tools, or we would use
        // a threadsafe POS tagger/chunker.
        post {
          entity(as[String]) { sentence =>
            val processed = app.process(sentence)
            val (types, extractions) = app(processed)
            complete(extractions.mkString("\n"))
          }
        }
      } ~
      // TODO(schmmd): add a "cascade" / level route to show taggers.
      path("cascade") {
        get {
          complete {
            cascade.levels map (_.name) mkString ("\n")
          }
        }
      } ~
      path("extractors") {
        get {
          complete {
            cascade.extractors mkString ("\n")
          }
        }
      } ~ infoRoute
    }
  }
}
