package edu.knowitall.taggers

import edu.knowitall.common.Resource
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.rule._
import edu.knowitall.taggers.tag._
import edu.knowitall.tool.typer.Type

import java.io.File
import java.io.Reader
import scala.io.Source

case class Level[-S <: Tagger.Sentence](taggers: Seq[Tagger[S]]) {
  def apply(sentence: S, types: Seq[Type]): Seq[Type] = {
    var levelTypes = Seq.empty[Type]

    for (tagger <- taggers) yield {
      val allTypes = types ++ levelTypes
      val taggerTypes = tagger(sentence, allTypes)

      levelTypes = levelTypes ++ taggerTypes

      if (tagger.isInstanceOf[ConsumingTagger[_]]) {
        taggerTypes foreach (sentence.consume(_))
      }
    }

    levelTypes
  }
}

object Level {
  def fromFile[S <: Tagger.Sentence](file: File): Level[S] = {
    Resource.using(Source.fromFile(file)) { input =>
      this.fromReader[S](input.bufferedReader)
    }
  }

  def fromReader[S <: Tagger.Sentence](reader: Reader): Level[S] = {
    this.fromParsed(new RuleParser[S].parse(reader).get)
  }

  def fromString[S <: Tagger.Sentence](string: String): Level[S] = {
    this.fromParsed(new RuleParser[S].parse(string).get)
  }

  def fromParsed[S <: Tagger.Sentence](parsed: ParsedLevel[S]): Level[S] = {
    var definitions: Seq[DefinitionRule[S]] = Seq.empty
    var taggers: Seq[Tagger[S]] = Seq.empty

    for (rule <- parsed.rules) {
      rule match {
        case defn: DefinitionRule[S] => definitions = definitions :+ defn
        case rule: TaggerRule[S] => taggers = taggers :+ rule.instantiate(definitions)
      }
    }

    Level(taggers)
  }
}
