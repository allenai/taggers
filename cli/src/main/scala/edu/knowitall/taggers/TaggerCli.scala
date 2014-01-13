package edu.knowitall.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type
import java.io.File
import scala.io.Source
import edu.knowitall.common.Resource.using

class TaggerApp(col: TaggerCollection[Sentence with Chunked with Lemmatized]) {
  type Sent = Sentence with Chunked with Lemmatized
  val chunker = new OpenNlpChunker()

  def format(typ: Type) = {
    typ.name + "(" + typ.text + ")"
  }

  def process(text: String): Sent = {
    new Sentence(text) with Chunker with Lemmatizer {
      val chunker = TaggerApp.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def apply(sentence: Sent) = {
    (col tag sentence).reverse map format
  }
}

object TaggerCliMain {
  case class Config(patternFile: File = null, sentencesFile: Option[File] = None) {
    def sentenceSource() = sentencesFile match {
      case Some(file) => Source.fromFile(file)
      case None => Source.fromInputStream(System.in)
    }

    def patternSource() = Source.fromFile(patternFile)
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("taggers") {
      arg[File]("<file>") action { (x, c) =>
        c.copy(patternFile = x)
      } text ("file specifying patterns")

      opt[File]('s', "sentences-file") action { (x, c) =>
        c.copy(sentencesFile = Some(x))
      } text ("file containing sentences")
    }

    parser.parse(args, Config()) match {
      case Some(config) => run(config)
      case None =>
    }
  }

  def run(config: Config): Unit = {
    def loadPatterns(text: String) = {
      // parse taggers
      val rules = new ParseRule[Sentence with Chunked with Lemmatized].parse(text).get

      // build a tagger collection
      val col = rules.foldLeft(new TaggerCollection[Sentence with Chunked with Lemmatized]()) {
        case (ctc, rule) =>
          ctc + rule
      }

      col
    }

    // load patterns
    val patterns =
      using(config.patternSource()) { source =>
        loadPatterns(source.getLines.mkString("\n"))
      }

    val app = new TaggerApp(patterns)

    using(config.sentenceSource()) { source =>
      // iterate over sentences
      for (line <- source.getLines) {
        val sentence = app.process(line)
        println(sentence)
        app(sentence) foreach println
        println()
      }
    }
  }
}
