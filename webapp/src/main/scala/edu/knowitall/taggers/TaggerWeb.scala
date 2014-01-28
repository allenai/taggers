package edu.knowitall.taggers

import edu.knowitall.common.Resource
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
import java.io.File
import scala.collection.immutable.IntMap
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import scala.io.Source
import org.apache.commons.lang3.StringEscapeUtils

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class TaggerWeb(ruleText: String, port: Int) {
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
      s"<br /><b>Rules:</b><br /><textarea id='patterns' name='patterns' cols='120' rows='20'>$patternText</textarea>" +
      s"<br /><b>Sentences:</b><br /><textarea id='sentences' name='sentences' cols='120' rows='20'>$sentenceText</textarea>" +
      """<br />
         <input type='submit'>""" +
      s"<p style='color:red'>${errors.mkString("<br />")}</p>" +
      s"<pre>${StringEscapeUtils.escapeHtml4(result)}</pre>" +
      """</form></body></html>"""
  }

  def run() {
    val plan = Planify {
      case req @ POST(Params(params)) => ResponseString(post(params))
      case req @ GET(_) => ResponseString(page(Map("patterns" -> Seq(ruleText))))
    }

    unfiltered.jetty.Http(port).filter(plan).run()
    System.out.println("Server started on port: " + port);
  }

  def post(params: Map[String, Seq[String]]) = {
    try {
      val sentenceText = params("sentences").headOption.get
      val patternText = params("patterns").headOption.get
      
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

      val linesIt = patternText.split("\n").iterator.buffered
      var sections = Seq.empty[String]
      var Seperator = "(?m)^>>>\\s*(.*)\\s*$".r
      var EmptyLine = "(?m)^\\s*$".r
      if (linesIt.hasNext) {
        var firstLoop = true
        while (linesIt.hasNext) {
          // consume empty lines
          dropWhile(linesIt, (line: String) => EmptyLine.pattern.matcher(line).matches)

          // match the header
          linesIt.head match {
            case Seperator(header) => linesIt.next()
            case _ if firstLoop => // there may be no header
            case _ => throw new MatchError("Header not found, rather: " + linesIt.head)
          }

          val body = takeWhile(linesIt, (line: String) => !Seperator.pattern.matcher(line).matches)
          sections = sections :+ body.mkString("\n")
          
          firstLoop = false
        }
      }

      val taggers: Seq[Seq[Tagger[MySentence]]] =
        sections map (text => Taggers.fromRules(new RuleParser[MySentence].parse(text).get))
      val levels: Seq[(Int, Seq[Tagger[MySentence]])] =
        taggers.zipWithIndex map (_.swap)
      val cascade = new Cascade[MySentence](IntMap(levels :_*))

      val results = for (line <- sentenceText.split("\n")) yield {
        val sentence = process(line)
        val levels = cascade.levels(sentence)

        (line, levels)
      }

      def formatType(typ: Type) = {
        typ.name + "(" + typ.text + ")"
      }
      val resultText = new StringBuilder()
      for ((line, levels) <- results) {
        resultText.append(line)
        resultText.append("\n\n")
        for ((level, types) <- levels.toSeq.reverse) {
          if (levels.size > 1) {
            resultText.append(s"  Level $level\n\n")
          }
          
          for (typ <- types.reverse) {
            resultText.append("  ")
            resultText.append(formatType(typ))
            resultText.append("\n")
          }
          
          resultText.append("\n")
        }
      }

      page(params, Seq.empty, resultText.toString)
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
  case class Config(inputFile: Option[File] = None, port: Int = 8080) {
    def ruleText() = inputFile match {
      case Some(file) => 
        val cascade = Cascade.partialLoad(file)
        val mapped = cascade map { case (level, entry) =>
          s">>> $level: ${entry.filename}\n\n${entry.text}"
        }
        mapped.mkString("\n\n\n")
      case None => ""
    }
  }
  val parser = new scopt.OptionParser[Config]("taggerweb") {
    opt[File]('c', "cascade").action { (file, c) =>
      c.copy(inputFile = Some(file))
    }.text("cascade file to pre-populate")
    opt[Int]('p', "port").action { (x, c) =>
      c.copy(port = x)
    }.text("port for web server")
  }

  parser.parse(args, Config()).foreach { config =>
    val server = new TaggerWeb(config.ruleText(), config.port)
    server.run()
  }
}
