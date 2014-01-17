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
import scala.collection.immutable.IntMap

/** Represents a sequence of taggers applied in order.
  * After each level of taggers, PatternTaggers can only use
  * type information from previous levels.
  *
  * @param  taggers  stores the taggers applied on each level
  */
case class Cascade[-S <: Sentence](taggers: IntMap[Seq[Tagger[S]]]) {
  lazy val chunker = new OpenNlpChunker()

  /** Convenience constructor to make an empty Cascade. */
  def this() = this(IntMap.empty[Seq[Tagger[S]]])

  /** Convenience constructor for a single tagger level. */
  def this(taggers: Seq[Tagger[S]]) = this(IntMap(0 -> taggers))

  /** Add a tagger to a particular level. */
  def plus[SS <: S](level: Int, tagger: Tagger[SS]): Cascade[SS] = {
    val entry = taggers.get(level).getOrElse(Seq.empty) :+ tagger
    Cascade[SS](taggers + (level -> entry))
  }

  /** Add taggers to a particular level. */
  def plus[SS <: S](level: Int, taggers: Seq[Tagger[SS]]): Cascade[SS] = {
    var cascade: Cascade[SS] = this
    for (tagger <- taggers) {
      cascade = cascade.plus(level, tagger)
    }

    cascade
  }

  /** Apply the cascade to a sentence.
    *
    * @returns  the found types
    */
  def apply(sentence: S): Seq[Type] = {
    val levels = taggers.toSeq.sortBy(_._1)

    var previousLevelTags = Seq.empty[Type]
    for ((level, taggers) <- levels) {
      var levelTags = Seq.empty[Type]
      val consumedIndices = previousLevelTags.map(_.tokenInterval).flatten

      for (tagger <- taggers) yield {
        val allTags = previousLevelTags ++ levelTags
        levelTags = levelTags ++ tagger(sentence, allTags, consumedIndices)
      }

      previousLevelTags = levelTags
    }

    previousLevelTags
  }
}
