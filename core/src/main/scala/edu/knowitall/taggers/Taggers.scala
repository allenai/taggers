package edu.knowitall.taggers

import edu.knowitall.common.Resource
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.rule._
import edu.knowitall.taggers.tag._

import java.io.File
import java.io.Reader
import scala.io.Source

object Taggers {
  def fromFile[S <: Sentence](file: File): Seq[Tagger[S]] = {
    Resource.using(Source.fromFile(file)) { input =>
      this.fromReader[S](input.bufferedReader)
    }
  }

  def fromReader[S <: Sentence](reader: Reader): Seq[Tagger[S]] = {
    this.fromRules(new RuleParser[S].parse(reader).get)
  }

  def fromString[S <: Sentence](string: String): Seq[Tagger[S]] = {
    this.fromRules(new RuleParser[S].parse(string).get)
  }

  def fromRules[S <: Sentence](rules: List[Rule[S]]): Seq[Tagger[S]] = {
    var definitions: Seq[DefinitionRule[S]] = Seq.empty
    var taggers: Seq[Tagger[S]] = Seq.empty

    for (rule <- rules) {
      rule match {
        case defn: DefinitionRule[S] => definitions = definitions :+ defn
        case rule: TaggerRule[S] => taggers = taggers :+ rule.instantiate(definitions)
      }
    }

    taggers
  }
}
