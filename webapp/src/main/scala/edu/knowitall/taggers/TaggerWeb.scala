package edu.knowitall.taggers

import edu.knowitall.common.Resource
import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.rule._
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.tag.PatternTagger
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

import akka.actor._
import spray.http._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing._

import java.io.File
import scala.collection.immutable.IntMap
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{ Try, Success, Failure }

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class TaggerWeb(taggersText: String, extractorText: String, sentenceText: String, port: Int) extends SimpleRoutingApp with SprayJsonSupport {
  // A type alias for convenience since TaggerWeb always
  // deals with sentences that are chunked and lemmatized
  type Sent = Tagger.Sentence with Chunks with Lemmas

  // External NLP tools that are used to build the expected type from a sentence string.
  lazy val chunker = new OpenNlpChunker()

  /** Build the NLP representation of a sentence string. */
  def process(text: String): Sent = {
    new Sentence(text) with Consume with Chunker with Lemmatizer {
      val chunker = TaggerWeb.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def run() {
    val staticContentRoot = "public"

    val cacheControlMaxAge = HttpHeaders.`Cache-Control`(CacheDirectives.`max-age`(60))

    implicit val system = ActorSystem("tagger-web")

    import DefaultJsonProtocol._
    implicit val requestFormat = jsonFormat3(Request.apply)

    implicit val tokenResponseFormat = jsonFormat7(TokenResponse.apply)
    implicit val extractorResponseFormat = jsonFormat2(ExtractorResponse.apply)
    implicit val levelResponseFormat = jsonFormat3(LevelResponse.apply)
    implicit val sentenceResponseFormat = jsonFormat3(SentenceResponse.apply)
    implicit val responseFormat = jsonFormat1(Response.apply)

    implicit val throwableWriter = new RootJsonWriter[Throwable] {
      /** Write a throwable as an object with 'message' and 'stackTrace' fields. */
      def write(t: Throwable) = {
        def getMessageChain(throwable: Throwable): List[String] = {
          Option(throwable) match {
            case Some(throwable) => Option(throwable.getMessage) match {
              case Some(message) => (throwable.getClass + ": " + message) :: getMessageChain(throwable.getCause)
              case None => getMessageChain(throwable.getCause)
            }
            case None => Nil
          }
        }
        val stackTrace = {
          val stackTraceWriter = new java.io.StringWriter()
          t.printStackTrace(new java.io.PrintWriter(stackTraceWriter))
          stackTraceWriter.toString
        }
        JsObject(
          "messages" -> JsArray(getMessageChain(t) map (JsString(_))),
          "stackTrace" -> JsString(stackTrace))
      }
    }

    implicit def exceptionHandler(implicit log: spray.util.LoggingContext) =
        ExceptionHandler {
          case e: Throwable => ctx =>
            // log in akka, which is configured to use slf4j
            log.error(e, "Unexpected Error.")

            // return the error formatted as json
            ctx.complete((InternalServerError, e.toJson.prettyPrint))
        }


    startServer(interface = "0.0.0.0", port = port) {
      import MediaTypes._
      handleExceptions(exceptionHandler) {
        respondWithHeader(cacheControlMaxAge) {
          path ("") {
            get {
              getFromFile(staticContentRoot + "/index.html")
            } ~
            post {
              entity(as[Request]) { request =>
                complete {
                  doPost(request)
                }
              }
            }
          } ~
          path ("fields") {
            get {
              complete {
                Request(taggersText, extractorText, sentenceText)
              }
            }
          } ~
          unmatchedPath { p => getFromFile(staticContentRoot + p) }
        }
      }
    }
  }

  case class Request(taggers: String, extractors: String, sentences: String)

  case class Response(sentences: Seq[SentenceResponse])
  case class TokenResponse(text: String, lemma: String, postag: String, chunk: String, consumed: Boolean, inputTypes: Seq[String], outputTypes: Seq[String])
  case class SentenceResponse(text: String, levels: Seq[LevelResponse], extractors: Seq[ExtractorResponse])
  case class LevelResponse(name: String, tokens: Seq[TokenResponse], types: Seq[String])
  case class ExtractorResponse(extractor: String, extractions: Seq[String])

  def doPost(request: Request): Response = {
    def takeWhile[T](it: BufferedIterator[T], cond: T=>Boolean): Seq[T] = {
      var result = Seq.empty[T]
      while (it.hasNext && cond(it.head)) {
        result = result :+ it.head

        it.next()
      }

      result
    }

    def dropWhile[T](it: BufferedIterator[T], cond: T=>Boolean): Unit = {
      while (it.hasNext && cond(it.head)) {
        it.next()
      }
    }

    val linesIt = request.taggers.split("\n").iterator.buffered
    var sections = Seq.empty[(String, String)]
    var Seperator = "(?m)^>>>\\s*(.*)\\s*$".r
    var EmptyLine = "(?m)^\\s*$".r
    if (linesIt.hasNext) {
      var firstLoop = true

      // Read the levels.
      while (linesIt.hasNext) {
        // consume empty lines
        dropWhile(linesIt, (line: String) => EmptyLine.pattern.matcher(line).matches)

        // match the header
        val header = linesIt.head match {
          case Seperator(h) => linesIt.next(); h
          case _ if firstLoop => "anonymous" // there may be no header
          case _ => throw new MatchError("Header not found, rather: " + linesIt.head)
        }

        val body = takeWhile(linesIt, (line: String) => !Seperator.pattern.matcher(line).matches)
        sections = sections :+ (header, body.mkString("\n"))

        firstLoop = false
      }
    }

    val levels: Seq[Level[Sent]] =
      sections map { case (title, text) => Level.fromString(title, text) }

    val extractorParser = new ExtractorParser()
    val extractors = for (line <- request.extractors.split("\n") filter (!_.trim.isEmpty)) yield {
      extractorParser.parse(line).get
    }

    val cascade = new Cascade[Sent](levels, extractors)

    val results = for (line <- request.sentences.split("\n")) yield {
      val sentence = process(line)
      val levels = cascade.levelTypes(sentence)

      (sentence, levels)
    }

    def formatType(typ: Type) = {
      typ.name + "(" + typ.text + ")"
    }

    val sentenceResponses = for {
      (sentence, levels) <- results
    } yield {
      var allTypes = Set.empty[Type] // collection of types seen so far
      val levelResponses = for {
        (level, types) <- levels.toSeq
      } yield {
        val tokens = PatternTagger.buildTypedTokens(sentence, allTypes)
        val tokenResponses = for ((typedToken, index) <- tokens.zipWithIndex) yield {
          val inputTypes = typedToken.types.iterator.map(_.name).toSeq
          val consumingType = sentence.consumingTypes(index)
          val consumed = consumingType.isDefined
          val outTypes = types filter (_.tokenInterval contains index)
          val lemmatized = typedToken.token
          val token = lemmatized.token
          TokenResponse(token.string, lemmatized.lemma, token.postag, token.chunk, consumed, inputTypes, outTypes map (_.name))
        }

        // Update the types seen so far.
        allTypes = allTypes ++ types

        LevelResponse(level, tokenResponses, types.reverse map formatType)
      }

      val extractorResponses =
        cascade.extract(allTypes).toSeq map { case (extractor, extractions) =>
          ExtractorResponse(extractor, extractions)
        }

      SentenceResponse(sentence.text, levelResponses, extractorResponses)
    }

    Response(sentenceResponses)
  }
}

object TaggerWebMain extends App {
  case class Config(ruleInputFile: Option[File] = None, sentenceInputFile: Option[File] = None, port: Int = 8080) {
    def cascadeText(): (String, String) = ruleInputFile match {
      case Some(file) =>
        val (levels, extractors) = Cascade.partialLoad(file)
        val mapped = levels map { entry =>
          s">>> ${entry.filename}\n\n${entry.text}"
        }
        (extractors.mkString("\n"), mapped.mkString("\n\n\n"))
      case None => ("", "")
    }

    def sentenceText() = sentenceInputFile match {
      case Some(file) =>
        Resource.using(Source.fromFile(file)) { source =>
          source.getLines.mkString("\n")
        }
      case None => ""
    }
  }

  val parser = new scopt.OptionParser[Config]("taggerweb") {
    opt[File]('c', "cascade").action { (file, c) =>
      c.copy(ruleInputFile = Some(file))
    }.text("cascade file to pre-populate")
    opt[File]('s', "sentences").action { (file, c) =>
      c.copy(sentenceInputFile = Some(file))
    }.text("sentence file to pre-populate")
    opt[Int]('p', "port").action { (x, c) =>
      c.copy(port = x)
    }.text("port for web server")
  }

  parser.parse(args, Config()).foreach { config =>
    val (extractorText, taggersText) = config.cascadeText()
    val server = new TaggerWeb(taggersText, extractorText, config.sentenceText(), config.port)
    server.run()
  }
}
