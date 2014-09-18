package org.allenai.taggers

import java.io.File
import java.io.Reader

import scala.io.Source
import scala.util.control.NonFatal

import org.allenai.nlpstack.core.typer.Type
import org.allenai.taggers.rule.DefinitionRule
import org.allenai.taggers.rule.ParsedLevel
import org.allenai.taggers.rule.RuleParser
import org.allenai.taggers.rule.TaggerRule
import org.allenai.taggers.tag.ConsumingTagger
import org.allenai.taggers.tag.Tagger

import edu.knowitall.common.Resource

case class Level[-S <: Tagger.Sentence](name: String, taggers: Seq[Tagger[S]]) {
  def apply(sentence: S, types: Seq[Type]): Seq[Type] = {
    var levelTypes = Seq.empty[Type]

    for (tagger <- taggers) yield {
      val allTypes = types ++ levelTypes
      val taggerTypes = tagger(sentence, allTypes)

      levelTypes = levelTypes ++ taggerTypes

      if (tagger.isInstanceOf[ConsumingTagger[_]]) {
        (taggerTypes filter (_.name == tagger.name)) foreach (sentence.consume(_))
      }
    }

    levelTypes
  }

  def typecheck(definedTypes: Set[String]): Unit = {
    var allDefinedTypes = definedTypes
    for (tagger <- taggers) {
      try {
        tagger.typecheck(allDefinedTypes)
      } catch {
        case NonFatal(e) =>
          throw new IllegalArgumentException(s"${tagger.name} contains undefined type.", e)
      }

      allDefinedTypes += tagger.name
    }
  }
}

object Level {
  def fromFile[S <: Tagger.Sentence](name: String, file: File): Level[S] = {
    Resource.using(Source.fromFile(file)) { input =>
      this.fromReader[S](name, input.bufferedReader)
    }
  }

  def fromReader[S <: Tagger.Sentence](name: String, reader: Reader): Level[S] = {
    this.fromParsed(name, new RuleParser[S].parse(reader).get)
  }

  def fromString[S <: Tagger.Sentence](name: String, string: String): Level[S] = {
    this.fromParsed(name, new RuleParser[S].parse(string).get)
  }

  def fromParsed[S <: Tagger.Sentence](name: String, parsed: ParsedLevel[S]): Level[S] = {
    var definitions: Seq[DefinitionRule[S]] = Seq.empty
    var taggers: Seq[Tagger[S]] = Seq.empty

    for (rule <- parsed.rules) {
      rule match {
        case defn: DefinitionRule[S] => definitions = definitions :+ defn
        case rule: TaggerRule[S] => taggers = taggers :+ rule.instantiate(definitions)
      }
    }

    Level(name, taggers)
  }
}
