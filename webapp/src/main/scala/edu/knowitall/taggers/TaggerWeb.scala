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

import org.apache.commons.lang3.StringEscapeUtils
import unfiltered.filter.Planify
import unfiltered.request._
import unfiltered.response._

import java.io.File
import scala.collection.immutable.IntMap
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import scala.io.Source

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class TaggerWeb(ruleText: String, sentenceText: String, port: Int) {
  // NLP tools
  val chunker = new OpenNlpChunker()

  type MySentence = Sentence with Chunks with Lemmas

  def process(text: String): MySentence = {
    new Sentence(text) with Chunker with Lemmatizer {
      val chunker = TaggerWeb.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def buildTable(header: Seq[String], rows: Iterable[Seq[String]]) =
    buildColoredTable(header, rows map (items => (None, items)))

  def buildColoredTable(header: Seq[String], rows: Iterable[(Option[String], Seq[String])]) =
    "<table>" +
      "<tr>" + header.map("<th>" + _ + "</th>").mkString("") + "</tr>" +
      rows.map { case (color, items) =>
        "<tr>" + items.map( item =>
          "<td" + color.map(" style=\"background-color: " + _ + "\")").getOrElse("") + ">" + item + "</td>"
        ).mkString("") + "</tr>"
      }.mkString("") +
    "</table>"

  def page(params: Map[String, Seq[String]] = Map.empty, errors: Seq[String] = Seq.empty, result: String = "", tables: String = "") = {
    val sentenceText = params.get("sentences").flatMap(_.headOption).getOrElse("")
    val patternText = params.get("patterns").flatMap(_.headOption).getOrElse("")
    """<html><head><title>Tagger Web</title><script src="http://ajax.googleapis.com/ajax/libs/jquery/2.0.1/jquery.min.js"></script>
    <style type='text/css'>
      /* <![CDATA[ */
        .head {
          font-weight: bold;
          width: 150px;
          border-width: 1px;
          border-style: solid;
          border-color: lightgrey;
          padding: 4px;
          margin-bottom: 0px;
        }
        .item {
          width: 144px;
          border-width: 1px;
          border-style: solid;
          border-color: lightgrey;
          padding: 4px;
          padding-left: 10px;
          margin-bottom: 0px;
        }
        table {
          border-width: 1px;
          border-spacing: 0px;
          border-style: outset;
          border-color: gray;
          border-collapse: collapse;
          background-color: white;
        }
        th {
          border-width: 1px;
          padding: 6px;
          border-style: inset;
          border-color: gray;
          background-color: PowderBlue;
          -moz-border-radius: 0px 0px 0px 0px;
        }
        td {
          border-width: 1px;
          padding: 6px;
          border-style: inset;
          border-color: gray;
          background-color: white;
          -moz-border-radius: 0px 0px 0px 0px;
        }
      /* ]]> */
    </style>
    </head>
       <body><h1>Tagger Web</h1><form method='POST'><p><a href='#' onclick="javascript:$('#patterns').val('Animal := NormalizedKeywordTagger { \n  cat\n  dot\n  frog\n}\n\nDescribedAnimal := PatternTagger ( <pos=\'JJ\'>+ <type=\'Animal\'>+ )'); $('#sentences').val('The large black cat rested on the desk.\nThe frogs start to ribbit in the spring.')">example</a></p>""" +
      s"<br /><b>Rules:</b><br /><textarea id='patterns' name='patterns' cols='120' rows='20'>$patternText</textarea>" +
      s"<br /><b>Sentences:</b><br /><textarea id='sentences' name='sentences' cols='120' rows='20'>$sentenceText</textarea>" +
      """<br />
         <input type='submit'>""" +
      s"<p style='color:red'>${errors.mkString("<br />")}</p>" +
      s"<pre>${StringEscapeUtils.escapeHtml4(result)}</pre>" +
      s"$tables" +
      """</form></body></html>"""
  }

  def run() {
    val plan = Planify {
      case req @ POST(Params(params)) => ResponseString(post(params))
      case req @ GET(_) => ResponseString(page(Map("patterns" -> Seq(ruleText), "sentences" -> Seq(sentenceText))))
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

        (sentence, levels)
      }

      def formatType(typ: Type) = {
        typ.name + "(" + typ.text + ")"
      }
      val resultText = new StringBuilder()
      val tables = new StringBuilder("<h3>Sentence Debugging</h3>")
      for ((sentence, levels) <- results) {
        resultText.append(sentence.text)
        resultText.append("\n\n")
        var previousLevelTypes = Set.empty[Type]
        for ((level, types) <- levels.toSeq) {
          if (levels.size > 1) {
            resultText.append(s"  Level $level\n\n")
          }

          val tokens = PatternTagger.buildTypedTokens(sentence, previousLevelTypes)
          val table = buildTable(Seq("index", "string", "postag", "chunk", "in types", "out types"), tokens map { typed =>
            val outTypes = types filter (_.tokenInterval contains typed.index)
            Seq(typed.index.toString,
              typed.token.string,
              typed.token.postag,
              typed.token.chunk,
              typed.types map formatType mkString (", "),
              outTypes map formatType mkString (", "))
          })
          tables.append(s"<h4>$level: ${sentence.text}</h4>")
          tables.append("<p>")
          tables.append(table)
          tables.append("</p>")
          tables.append("\n\n")

          for (typ <- types.reverse) {
            resultText.append("  ")
            resultText.append(formatType(typ))
            resultText.append("\n")
          }

          resultText.append("\n")

          previousLevelTypes = types.toSet
        }
      }

      page(params, Seq.empty, resultText.toString, tables.toString)
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
  case class Config(ruleInputFile: Option[File] = None, sentenceInputFile: Option[File] = None, port: Int = 8080) {
    def ruleText() = ruleInputFile match {
      case Some(file) =>
        val cascade = Cascade.partialLoad(file)
        val mapped = cascade map { case (level, entry) =>
          s">>> $level: ${entry.filename}\n\n${entry.text}"
        }
        mapped.mkString("\n\n\n")
      case None => ""
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
    val server = new TaggerWeb(config.ruleText(), config.sentenceText(), config.port)
    server.run()
  }
}
