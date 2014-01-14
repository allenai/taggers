package edu.knowitall.taggers

import edu.knowitall.taggers.rule._
import edu.knowitall.common.Resource.using
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

import io.Source

import java.io.File
import java.io.FileReader
import java.io.Reader

case class Cascade[S <: Sentence](taggers: Seq[Tagger[S]]) {
  lazy val chunker = new OpenNlpChunker()

  def this() = this(Seq.empty)

  def +[SS <: S](tagger: Tagger[SS]): Cascade[SS] = {
    Cascade[SS](taggers :+ tagger)
  }

  def tag(sentence: S): Seq[Type] = {
    var tags = Seq.empty[Type]
    for (tagger <- this.taggers) {
      tags = tags ++ tagger.tags(sentence, tags)
    }
    tags
  }
}
