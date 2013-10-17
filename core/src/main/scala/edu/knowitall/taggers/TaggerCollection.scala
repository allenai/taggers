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
import edu.knowitall.repr.sentence.Sentence

object TaggerCollection {
  def fromFile[S <: Sentence](file: File): TaggerCollection[S] = {
    using(Source.fromFile(file)) { input =>
      this.fromReader[S](input.bufferedReader)
    }
  }

  def fromReader[S <: Sentence](reader: Reader): TaggerCollection[S] = {
    this.fromRules(new ParseRule[S].parse(reader).get)
  }

  def fromString[S <: Sentence](string: String): TaggerCollection[S] = {
    this.fromRules(new ParseRule[S].parse(string).get)
  }

  def fromRules[S <: Sentence](rules: List[Rule[S]]): TaggerCollection[S] = {
    val tc = new TaggerCollection[S]()
    rules.foldLeft(tc) { case (col, rule) => col + rule }
  }
}

case class TaggerCollection[S <: Sentence](taggers: Seq[Tagger[S]], definitions: Seq[DefinitionRule[S]]) {

  lazy val chunker = new OpenNlpChunker()

  def this() = this(Seq.empty, Seq.empty)

  def +[SS <: S](tagger: Tagger[SS]): TaggerCollection[SS] = {
    TaggerCollection[SS](taggers :+ tagger, definitions)
  }

  def +[SS <: S](rule: Rule[SS]): TaggerCollection[SS] = {
    rule match {
      case defn: DefinitionRule[SS] =>
        TaggerCollection[SS](taggers, definitions :+ defn)
      case rule: TaggerRule[SS] =>
        val tagger = rule.instantiate(definitions)
        this + tagger
    }
  }

  def tag(sentence: S): Seq[Type] = {
    var tags = Seq.empty[Type]
    for (tagger <- this.taggers) {
      tags = tags ++ tagger.tags(sentence, tags)
    }
    tags
  }
}