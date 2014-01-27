package edu.knowitall.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.rule._
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

import edu.knowitall.common.Resource.using

import java.io.File
import scala.io.Source

class TaggerApp(cascade: Cascade[Sentence with Chunks with Lemmas]) {
  type Sent = Sentence with Chunks with Lemmas
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
    (cascade apply sentence).reverse map format
  }
}

object TaggerCliMain {
  case class Config(cascadeFile: File = null, sentencesFile: Option[File] = None) {
    def sentenceSource() = sentencesFile match {
      case Some(file) => Source.fromFile(file)
      case None => Source.fromInputStream(System.in)
    }

    def cascadeSource() = Source.fromFile(cascadeFile)
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("taggers") {
      arg[File]("<file>") action { (x, c) =>
        c.copy(cascadeFile = x)
      } text ("file specifying cascade")

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
    def loadTaggers(text: String) = {
      // parse taggers
      val rules = new RuleParser[Sentence with Chunks with Lemmas].parse(text).get

      Taggers.fromRules(rules)
    }

    val cascade = Cascade.load(config.cascadeFile)

    val app = new TaggerApp(cascade)

    using(config.sentenceSource()) { source =>
      // iterate over sentences
      for (line <- source.getLines) {
        val sentence = app.process(line)
        app(sentence) foreach println
        println()
      }
    }
  }
}
