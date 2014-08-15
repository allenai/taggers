package org.allenai.taggers

import akka.actor._
import spray.http._
import spray.routing._

import java.io.File

/** This class creates a server that runs a cascade file.
  * POST requests can contain a single sentence.  The response
  * is the output of the extractors.
  */
object TaggerServerMain extends SimpleRoutingApp {
  /** A representation of the command-line parameters. */
  case class Config(port: Int = 8080, cascadeFile: File = null, extractorName: Option[String] = None, extractorDescription: Option[String] = None)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("taggers") {
      arg[File]("<file>") action { (x, c) =>
        c.copy(cascadeFile = x)
      } text ("file specifying cascade")

      opt[String]("name") optional() action { (x, c) =>
        c.copy(extractorName = Some(x))
      } text ("name of the extractor(s)")

      opt[String]("description") optional() action { (x, c) =>
        c.copy(extractorDescription = Some(x))
      } text ("description for the extractor(s)")

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
    // used for level routes
    val levelTextMap: Map[String, String] =
      (for (level <- Cascade.partialLoad(config.cascadeFile)._1)
      yield level.filename -> level.text)(scala.collection.breakOut)

    val cascade = Cascade.load(config.cascadeFile, config.cascadeFile.getName)
    val app = new ChunkedTaggerApp(cascade)

    val info: Map[String, String] = Map(
       "name" -> config.extractorName.getOrElse(cascade.name),
       "description" -> config.extractorDescription.getOrElse("")
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

    implicit def exceptionHandler(implicit log: spray.util.LoggingContext) =
      ExceptionHandler {
        case e: Throwable => ctx =>
          // log in akka, which is configured to use slf4j
          log.error(e, "Unexpected Error.")

          // return the error formatted as json
          ctx.complete((spray.http.StatusCodes.InternalServerError, e.getMessage))
      }

    startServer(interface = "0.0.0.0", port = config.port) {
      handleExceptions(exceptionHandler) {
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
        path("cascade") {
          get {
            complete {
              cascade.levels map (_.name) mkString ("\n")
            }
          }
        } ~
        path("cascade" / Segment) { level =>
          get {
            complete {
              levelTextMap map (_._1) foreach println
              levelTextMap.getOrElse(level,
                throw new IllegalArgumentException("Unknown level: " + level))
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
}
