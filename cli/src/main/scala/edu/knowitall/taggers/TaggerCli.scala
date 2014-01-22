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
    // load a cascade definition file
    def loadCascade(basePath: File, lines: Iterator[String]) = {
      System.err.println("Loading cascade: " + basePath)

      // paths inside are either absolute or relative to the cascade definition file
      def makeFile(path: String) = {
        val file = new File(path)
        if (file.isAbsolute) file
        else new File(basePath, path)
      }

      var cascade = new Cascade[Sentence with Chunks with Lemmas]()

      // Iterate over the level definitions, load the tagger files,
      // and add them to the cascade.
      val levels = for (
        line <- lines map (_.trim)
        if !line.isEmpty) {

        // A line is composed of a level number and a tagger file path
        val (level, taggerFile) = line.split("\t") match {
          case Array(levelString, taggerFilePath) =>
            (levelString.toInt, makeFile(taggerFilePath))
          case _ => throw new MatchError("Could not understand cascade line: " + line)
        }

        System.err.println("Loading taggers at level " + level + ": " + taggerFile)

        val taggers = using(Source.fromFile(taggerFile)) { source =>
          loadTaggers(source.getLines.mkString("\n"))
        }

        cascade = cascade.plus(level, taggers)
      }

      System.err.println("Done loading cascade.")
      System.err.println()

      cascade
    }

    def loadTaggers(text: String) = {
      // parse taggers
      val rules = new RuleParser[Sentence with Chunks with Lemmas].parse(text).get

      Taggers.fromRules(rules)
    }

    val cascade =
      using(config.cascadeSource()) { source =>
        loadCascade(config.cascadeFile.getParentFile, source.getLines)
      }

    val app = new TaggerApp(cascade)

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
