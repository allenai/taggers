package edu.knowitall.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.rule._
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

import unfiltered.filter.Planify
import unfiltered.request._
import unfiltered.response._

import scala.collection.immutable.IntMap
import scala.collection.JavaConverters._

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class TaggerWeb(port: Int) {
  // NLP tools
  val chunker = new OpenNlpChunker()

  type MySentence = Sentence with Chunks with Lemmas

  def process(text: String): MySentence = {
    new Sentence(text) with Chunker with Lemmatizer {
      val chunker = TaggerWeb.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def page(params: Map[String, Seq[String]] = Map.empty, errors: Seq[String] = Seq.empty, result: String = "") = {
    val sentenceText = params.get("sentences").flatMap(_.headOption).getOrElse("")
    val patternText = params.get("patterns").flatMap(_.headOption).getOrElse("")
    """<html><head><title>Tagger Web</title><script src="http://ajax.googleapis.com/ajax/libs/jquery/2.0.1/jquery.min.js"></script></head>
       <body><h1>Tagger Web</h1><form method='POST'><p><a href='#' onclick="javascript:$('#patterns').val('Animal := NormalizedKeywordTagger { \n  cat\n  dot\n  frog\n}\n\nDescribedAnimal := PatternTagger ( <pos=\'JJ\'>+ <type=\'Animal\'>+ )'); $('#sentences').val('The large black cat rested on the desk.\nThe frogs start to ribbit in the spring.')">example</a></p>""" +
      s"<br /><b>Patterns:</b><br /><textarea id='patterns' name='patterns' cols='120' rows='20'>$patternText</textarea>" +
      s"<br /><b>Sentences:</b><br /><textarea id='sentences' name='sentences' cols='120' rows='20'>$sentenceText</textarea>" +
      """<br />
         <input type='submit'>""" +
      s"<p style='color:red'>${errors.mkString("<br />")}</p>" +
      s"<pre>$result</pre>" +
      """</form></body></html>"""
  }

  def run() {
    val plan = Planify {
      case req @ POST(Params(params)) => ResponseString(post(params))
      case req @ GET(_) => ResponseString(page())
    }

    unfiltered.jetty.Http(port).filter(plan).run()
    System.out.println("Server started on port: " + port);
  }

  def post(params: Map[String, Seq[String]]) = {
    try {
      val sentenceText = params("sentences").headOption.get
      val patternText = params("patterns").headOption.get

      val sections = patternText split ("\\n\\s*>>>\\s*\\n")
      val taggers: Array[Seq[Tagger[MySentence]]] =
        sections map (text => Taggers.fromRules(new RuleParser[MySentence].parse(text).get))
      val levels: Array[(Int, Seq[Tagger[MySentence]])] =
        taggers.zipWithIndex map (_.swap)
      val cascade = new Cascade[MySentence](IntMap(levels :_*))

      val results = for (line <- sentenceText.split("\n")) yield {
        val sentence = process(line)
        val types = cascade.apply(sentence).reverse

        (line, types)
      }

      def formatType(typ: Type) = {
        typ.name + "(" + typ.text + ")"
      }
      val resultText =
        results.map {
          case (sentence, typs) =>
            sentence + "\n\n" + typs.map(formatType).mkString("\n")
        }.mkString("\n\n")

      page(params, Seq.empty, resultText)
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        def getMessageChain(throwable: Throwable): List[String] = {
          Option(throwable) match {
            case Some(throwable) => Option(throwable.getMessage) match {
              case Some(message) => (throwable.getClass + ": " + message) :: getMessageChain(throwable.getCause)
              case None => getMessageChain(throwable.getCause)
            }
            case None => Nil
          }
        }
        page(params, getMessageChain(e), "")
    }
  }
}

object TaggerWebMain extends App {
  case class Config(port: Int = 8080)
  val parser = new scopt.OptionParser[Config]("taggerweb") {
    opt[Int]('p', "port").action { (x, c) =>
      c.copy(port = x)
    }.text("port for web server")
  }

  parser.parse(args, Config()).foreach { config =>
    val server = new TaggerWeb(config.port)
    server.run()
  }
}
