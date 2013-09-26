package edu.knowitall.taggers

import scala.collection.JavaConverters._

import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.chunk.OpenNlpChunker

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.Planify

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class TaggerWeb(port: Int) {
  // NLP tools
  val chunker = new OpenNlpChunker()
  val stemmer = new MorphaStemmer()

  def page(params: Map[String, Seq[String]] = Map.empty, errors: Seq[String] = Seq.empty, result: String = "") = {
    val sentenceText = params.get("sentences").flatMap(_.headOption).getOrElse("")
    val patternText = params.get("patterns").flatMap(_.headOption).getOrElse("")
    """<html><head><title>Tagger Web</title></head>
       <body><h1>Tagger Web</h1><form method='POST'>""" +
         s"<br /><b>Patterns:</b><br /><textarea name='patterns' cols='120' rows='20'>$patternText</textarea>" +
         s"<br /><b>Sentences:</b><br /><textarea name='sentences' cols='120' rows='20'>$sentenceText</textarea>" +
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

      val rules = ParseRule.parse(patternText).get
      val ctc = rules.foldLeft(new CompactTaggerCollection()){ case (ctc, rule) => ctc + rule }
      val col = ctc.toTaggerCollection

      val results = for (line <- sentenceText.split("\n")) yield {
        val tokens = chunker(line) map stemmer.lemmatizeToken
        val types = col.tag(tokens.asJava).asScala

        (line, types)
      }

      val resultText = ctc.taggers.mkString("\n") + "\n\n" +
        results.map { case (sentence, typs) =>
          sentence + "\n" + typs.mkString("\n")
        }.mkString("\n\n")

      page(params, Seq.empty, resultText)
    }
    catch {
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
