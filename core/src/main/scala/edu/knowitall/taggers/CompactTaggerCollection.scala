package edu.knowitall.taggers

import io.Source
import java.io.File
import edu.knowitall.common.Resource.using
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.constraint.Constraint
import java.io.FileReader
import edu.knowitall.taggers.tag.TaggerCollection
import java.io.Reader

object CompactTaggerCollection {
  def fromFile(file: File) = {
    using(Source.fromFile(file)) { input =>
      this.fromReader(input.bufferedReader)
    }
  }

  def fromReader(reader: Reader) = {
    this.fromRules(ParseRule.parse(reader).get)
  }

  def fromString(string: String) = {
    this.fromRules(ParseRule.parse(string).get)
  }

  def fromRules(rules: List[Rule]) = {
    rules.foldLeft(new CompactTaggerCollection()) { case (col, rule) => col + rule }
  }
}

case class CompactTaggerCollection(taggers: Seq[Tagger], definitions: Seq[DefinitionRule]) {
  def this() = this(Seq.empty, Seq.empty)

  def +(tagger: Tagger): CompactTaggerCollection = {
    this.copy(taggers :+ tagger)
  }

  def +(rule: Rule): CompactTaggerCollection = {
    rule match {
      case defn @ DefinitionRule(_, _) =>
        this.copy(definitions = definitions :+ defn)
      case rule @ TaggerRule(descriptor, _, _, _) =>
        val tagger = rule.instantiate(definitions)
        this + tagger
    }
  }

  def toTaggerCollection: TaggerCollection = {
    val col = new TaggerCollection()

    taggers foreach col.addTagger

    col
  }
}