package edu.knowitall.taggers

import io.Source
import java.io.File
import edu.knowitall.common.Resource.using
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.constraint.Constraint
import java.io.FileReader
import java.io.Reader
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer

object TaggerCollection {

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
    rules.foldLeft(new TaggerCollection()) { case (col, rule) => col + rule }
  }
}

case class TaggerCollection(taggers: Seq[Tagger], definitions: Seq[DefinitionRule]) {
  
  lazy val chunker = new OpenNlpChunker()
  
  def this() = this(Seq.empty, Seq.empty)

  def +(tagger: Tagger): TaggerCollection = {
    this.copy(taggers :+ tagger)
  }

  def +(rule: Rule): TaggerCollection = {
    rule match {
      case defn @ DefinitionRule(_, _) =>
        this.copy(definitions = definitions :+ defn)
      case rule @ TaggerRule(name, _, _, _) =>
        val tagger = rule.instantiate(definitions)
        this + tagger
    }
  }

  def tag(sentence: Seq[Lemmatized[ChunkedToken]]): Seq[Type] = {
    var tags = Seq.empty[Type]
    for (tagger <- this.taggers) {
      tags = tags ++ tagger.tags(sentence, tags)
    }
    tags
  }
  
  def tag(sentence: String): Seq[Type] = {
    val processedSentence = chunker.chunk(sentence) map MorphaStemmer.lemmatizeToken
    this.tag(processedSentence)
  }
}