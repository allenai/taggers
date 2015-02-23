package org.allenai.taggers

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr._
import org.allenai.nlpstack.core.typer.Type
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.pipeline._
import org.allenai.taggers.Cascade.LevelDefinition
import org.allenai.taggers.tag.{OpenRegex, Tagger}
import org.allenai.pipeline.IoHelpers._

import akka.actor._
import org.allenai.common.Resource
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing._

import scala.io.Source
import java.io.File

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class TaggerWeb(
    levelDefinitions: Seq[LevelDefinition] = TaggerWeb.defaultLevels,
    extractorText: String = TaggerWeb.defaultExtractorText,
    sentences: Producer[Iterable[Tagger.Sentence with Chunks with Lemmas]],
    port: Int)
  extends SimpleRoutingApp with SprayJsonSupport {

  // A type alias for convenience since TaggerWeb always
  // deals with sentences that are chunked and lemmatized
  type Sent = Tagger.Sentence with Chunks with Lemmas

  // External NLP tools that are used to build the expected type from a sentence string.
  def run() {
    val staticContentRoot = "public"

    val cacheControlMaxAge = HttpHeaders.`Cache-Control`(CacheDirectives.`max-age`(60))

    implicit val system = ActorSystem("tagger-web")

    import spray.json.DefaultJsonProtocol._
    implicit val levelFormat = jsonFormat2(LevelDefinition.apply)
    implicit val requestFormat = jsonFormat3(Request.apply)

    implicit val typeFormat = new JsonFormat[Type] {
      def write(typ: Type): JsValue = {
        JsObject(
          "name" -> JsString(typ.name),
          "text" -> JsString(typ.text),
          "startIndex" -> JsNumber(typ.tokenInterval.start),
          "endIndex" -> JsNumber(typ.tokenInterval.end))
      }

      // This unfortunately exists to support the following jsonFormat calls.
      def read(value: JsValue): Type = throw new UnsupportedOperationException()
    }
    implicit val tokenResponseFormat = jsonFormat7(TokenResponse.apply)
    implicit val extractorResponseFormat = jsonFormat2(ExtractorResponse.apply)
    implicit val levelResponseFormat = jsonFormat3(LevelResponse.apply)
    implicit val sentenceResponseFormat = jsonFormat4(SentenceResponse.apply)
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
          "messages" -> null, //JsArray(getMessageChain(t) map (JsString(_))),
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
                val sentencesText = (sentences.get map (_.text)).mkString("\n")
                Request(
                    levelDefinitions, 
                    extractorText, 
                    sentencesText)
              }
            }
          } ~
          unmatchedPath { p => getFromFile(staticContentRoot + p) }
        }
      }
    }
  }

  case class Request(levelDefinitions: Seq[LevelDefinition], extractors: String, sentences: String)

  case class Response(sentences: Seq[SentenceResponse])
  case class TokenResponse(text: String, lemma: String, postag: String, chunk: String, consumed: Boolean, inputTypes: Seq[String], outputTypes: Seq[String])
  case class SentenceResponse(text: String, tokens: Seq[String], levels: Seq[LevelResponse], extractors: Seq[ExtractorResponse])
  case class LevelResponse(name: String, tokens: Seq[TokenResponse], types: Seq[Type])
  case class ExtractorResponse(extractor: String, extractions: Seq[String])

  def doPost(request: Request): Response = {
    val levels: Seq[Level[Sent]] =
      request.levelDefinitions map { case LevelDefinition(title, text) => Level.fromString(title, text) }

    val extractorParser = new ExtractorParser()
    val extractors = for (line <- request.extractors.split("\n") filter (!_.trim.isEmpty)) yield {
      extractorParser.parse(line).get
    }

    val cascade = new Cascade[Sent]("webapp", levels, extractors)

    val results = for (sentence <- sentences.get.toList) yield {
      val levels = cascade.levelTypes(sentence)

      (sentence, levels)
    }

    val sentenceResponses = for {
      (sentence, levels) <- results
    } yield {
      var allTypes = Seq.empty[Type] // collection of types seen so far
      val levelResponses = for {
        (level, types) <- levels.toSeq
      } yield {
        val tokens = OpenRegex.buildTypedTokens(sentence, allTypes)
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

        LevelResponse(level, tokenResponses, types.reverse)
      }

      val extractorResponses =
        cascade.extract(allTypes).toSeq map { case (extractor, extractions) =>
          ExtractorResponse(extractor, extractions)
        }

      SentenceResponse(sentence.text, sentence.tokens.map(_.string), levelResponses, extractorResponses)
    }

    Response(sentenceResponses)
  }
}

object TaggerWeb {
  val defaultLevels = Seq(LevelDefinition("Anonymous",
  """|Animal := LemmatizedKeywordTagger {
     |cat
     |bird
     |frog
   |}

   |DescribedAnimal := TypedOpenRegex ( <pos='JJ'>+ @Animal )""".stripMargin))
   val defaultExtractorText = "x: DescribedAnimal => described animal: ${x}"
   val defaultCascade = (defaultExtractorText, defaultLevels)
   val defaultSentenceText =
      """|The fat black cat was hidden in the dark corner.
         |The song birds sing new songs in the spring.""".stripMargin
  }

object TaggerWebMain extends App {
  case class Config(
      ruleInputFile: Option[File] = None,
      sentenceInputFile: Option[File] = None,
      port: Int = 8080) {
    def cascade(): (String, Seq[LevelDefinition]) = ruleInputFile match {
      case Some(file) =>
        val (levels, extractors) = Cascade.partialLoad(file)
        (extractors.mkString("\n"), levels)
      case None => TaggerWeb.defaultCascade
    }

    def sentenceText() = sentenceInputFile match {
      case Some(file) =>
        Resource.using(Source.fromFile(file)) { source =>
          source.getLines.mkString("\n")
        }
      case None => TaggerWeb.defaultSentenceText
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
  
  /*
  .fromArtifact[Sentence](
      new FileArtifact(inputSentences))
    val allInstances = PersistedIterator.text(extractionFile)(
      new ExtractInstances(sentences, confidenceThreshold))
    val filteredInstances = new FilterInstances(allInstances, confidenceThreshold)
    val workingInstances = PersistedIterator.text(filteredExtractionFile) {
      blacklistFile match {
        case Some(f) => {
          val blacklist = ReadCollection.text[String](new FileArtifact(f))
          new FilterInstancesFromBlacklist(filteredInstances, blacklist)
        }
        case None => filteredInstances
      }
    }
    */
  
  def addExtension(file: File, suffix: String): String = {
    val fileName = file.getName
    val dot = fileName.lastIndexOf('.')
    if (dot < 0) {
      s"$fileName.$suffix"
    }
    else {
      s"${fileName.substring(0, dot)}.$suffix.${fileName.substring(dot + 1)}"
    }
  }

  parser.parse(args, Config()).foreach { config =>
    val chunked = config.sentenceInputFile.map { file =>
      val output = new RelativeFileSystem(file.getParentFile)
      val sentences = IoHelpers.Read.Collection.fromText[String](new FileArtifact(file))
      new Pipelines.ChunkSentences(sentences).persisted(
          Pipelines.chunkedSentencesArtifactIo,
          output.flatArtifact(addExtension(file, "chunked"))
      )
    }.getOrElse {
      IoHelpers.FromMemory(Iterable.empty[Pipelines.ChunkedSentence])
    }
    val (extractorText, levelDefinitions) = config.cascade()
    val server = new TaggerWeb(levelDefinitions, extractorText, chunked, config.port)
    server.run()
  }
}
