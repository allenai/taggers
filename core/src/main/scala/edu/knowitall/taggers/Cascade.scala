package edu.knowitall.taggers

import edu.knowitall.taggers.rule._
import edu.knowitall.common.Resource.using
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.constraint.Constraint
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
import scala.collection.immutable

/** Represents a sequence of taggers applied in order.
  * After each level of taggers, PatternTaggers can only use
  * type information from previous levels.
  *
  * @param  levels  stores the taggers applied on each level
  */
case class Cascade[-S <: Sentence](levels: Seq[Level[S]]) {
  lazy val chunker = new OpenNlpChunker()

  /** Convenience constructor to make an empty Cascade. */
  def this() = this(Seq.empty[Level[S]])

  /** Convenience constructor to make a Cascade with a single Level. */
  def this(level: Level[S]) = this(Seq(level))

  /** Add a tagger level. */
  def +[SS <: S](level: Level[SS]): Cascade[SS] = {
    Cascade[SS](levels :+ level)
  }

  /** Apply the cascade to a sentence.
    *
    * @returns  the found types
    */
  def apply(sentence: S): Seq[Type] = {
    var previousTypes = Seq.empty[Type]
    var definedTypes = Set.empty[String]
    var previousLevelTypes = Seq.empty[Type]
    for (level <- levels) {
      level.typecheck(definedTypes)
      definedTypes ++= (level.taggers.iterator map (_.name)).toSet

      val levelTypes = level.apply(sentence, previousTypes)

      previousTypes ++= levelTypes
      previousLevelTypes = levelTypes
    }

    previousLevelTypes
  }

  /** Apply the cascade to a sentence, keeping types found at all levels.
    *
    * @returns  the found types at each level
    */
  def levelTypes(sentence: S): immutable.ListMap[Int, Seq[Type]] = {
    var result = immutable.ListMap.empty[Int, Seq[Type]]
    var previousLevelTags = Seq.empty[Type]
    for ((level, index) <- levels.zipWithIndex) {
      val levelTags = level.apply(sentence, previousLevelTags)

      result += index -> levelTags
      previousLevelTags = levelTags
    }

    result
  }
}

object Cascade {
  case class TaggerEntry(filename: String, text: String)

  // load a cascade definition file
  def load[S <: Sentence](cascadeFile: File): Cascade[S] = {
    using(Source.fromFile(cascadeFile)) { source =>
      load(cascadeFile.getParentFile, source)
    }
  }

  def load[S <: Sentence](basePath: File, cascadeSource: Source): Cascade[S] = {
    System.err.println("Loading cascade definition: " + basePath)

    var cascade = new Cascade[S]()
    for (TaggerEntry(filename, text) <- partialLoad(basePath, cascadeSource.getLines)) {
      System.err.println("Parsing taggers from: " + filename)
      cascade = cascade + Taggers.fromString(text)
    }

    System.err.println("Done loading cascade.")
    System.err.println()

    cascade
  }

  def partialLoad(cascadeFile: File): Seq[TaggerEntry] = {
    using(Source.fromFile(cascadeFile)) { source =>
      partialLoad(cascadeFile.getParentFile, source)
    }
  }

  def partialLoad(basePath: File, cascadeSource: Source): Seq[TaggerEntry] = {
    partialLoad(basePath, cascadeSource.getLines)
  }

  private def partialLoad(basePath: File, lines: Iterator[String]): Seq[TaggerEntry] = {
    // paths inside are either absolute or relative to the cascade definition file
    def makeFile(path: String) = {
      val file = new File(path)
      if (file.isAbsolute) file
      else new File(basePath, path)
    }

    // Iterate over the level definitions, load the tagger files,
    // and add them to the cascade.
    val levels = for (
      line <- lines map (_.trim) if !line.isEmpty
    ) yield {

      // A line is composed of a level number and a tagger file path
      val taggerFilePath = line
      val taggerFile = makeFile(taggerFilePath)

      System.err.println("Loading taggers from: " + taggerFile)

      val taggerText = using(Source.fromFile(taggerFile)) { source =>
        source.getLines.mkString("\n")
      }

      TaggerEntry(taggerFile.getName, taggerText)
    }

    Seq.empty[TaggerEntry] ++ levels
  }
}
