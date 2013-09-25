package edu.knowitall.taggers

import io.Source
import java.io.File
import edu.knowitall.common.Resource.using
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.constraint.Constraint
import java.io.FileReader
import edu.knowitall.taggers.tag.TaggerCollection

object CompactTaggerCollection {
  def fromFile(file: File) = {
    using(Source.fromFile(file)) { input =>
    }
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