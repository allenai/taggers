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

/** Represents a sequence of taggers applied in order.
  * Between each level of taggers, all typed tokens lose all information
  * except type information.
  *
  * @param  taggers  stores the taggers applied on each level
  */
case class Cascade[S <: Sentence](taggers: Map[Int, Seq[Tagger[S]]]) {
  lazy val chunker = new OpenNlpChunker()

  def this() = this(Map.empty[Int, Seq[Tagger[S]]])

  /** Convenience constructor for a single tagger level. */
  def this(taggers: Seq[Tagger[S]]) = this(Map(0 -> taggers))

  def add[SS <: S](level: Int, tagger: Tagger[SS]): Cascade[SS] = {
    val entry = taggers.get(level).getOrElse(Seq.empty) :+ tagger
    Cascade[SS](taggers + (level -> entry))
  }

  def tag(sentence: S): Seq[Type] = {
    val levels = taggers.toSeq.sortBy(_._1)

    var previousLevelTags = Seq.empty[Type]
    for ((level, taggers) <- levels) {
      var levelTags = Seq.empty[Type]
      for (tagger <- taggers) yield {
        val allTags = previousLevelTags ++ levelTags
        val previousIndices = previousLevelTags.map(_.tokenInterval).flatten
        levelTags = levelTags ++ tagger.tags(sentence, allTags, previousIndices)
      }

      previousLevelTags ++= levelTags
    }

    previousLevelTags
  }
}
