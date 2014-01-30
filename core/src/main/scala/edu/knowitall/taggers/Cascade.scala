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
import scala.collection.immutable.ListMap

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

  /** Apply the cascade to a sentence, keeping types found at all levels.
    *
    * @returns  the found types at each level
    */
  def levels(sentence: S): ListMap[Int, Seq[Type]] = {
    val levels = taggers.toSeq.sortBy(_._1)

    var result = ListMap.empty[Int, Seq[Type]]
    var previousLevelTags = Seq.empty[Type]
    for ((level, taggers) <- levels) {
      var levelTags = Seq.empty[Type]
      val consumedIndices = previousLevelTags.map(_.tokenInterval).flatten

      for (tagger <- taggers) yield {
        val allTags = previousLevelTags ++ levelTags
        levelTags = levelTags ++ tagger(sentence, allTags, consumedIndices)
      }
      
      result += level -> levelTags
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
    def loadTaggers(text: String) = {
      // parse taggers
      val rules = new RuleParser[S].parse(text).get

      Taggers.fromRules(rules)
    }

    System.err.println("Loading cascade definition: " + basePath)

    var cascade = new Cascade[S]()
    for ((level, TaggerEntry(filename, text)) <- partialLoad(basePath, cascadeSource.getLines)) {
      System.err.println("Parsing taggers at level " + level + ": " + filename)
      cascade = cascade.plus(level, loadTaggers(text))
    }

    System.err.println("Done loading cascade.")
    System.err.println()
    
    cascade
  }

  def partialLoad(cascadeFile: File): IntMap[TaggerEntry] = {
    using(Source.fromFile(cascadeFile)) { source =>
      partialLoad(cascadeFile.getParentFile, source)
    }
  }

  def partialLoad(basePath: File, cascadeSource: Source): IntMap[TaggerEntry] = {
    partialLoad(basePath, cascadeSource.getLines)
  }

  private def partialLoad(basePath: File, lines: Iterator[String]): IntMap[TaggerEntry] = {
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
      val (level, taggerFile) = line.split("\t") match {
        case Array(levelString, taggerFilePath) =>
          (levelString.toInt, makeFile(taggerFilePath))
        case _ => throw new MatchError("Could not understand cascade line: " + line)
      }

      System.err.println("Loading taggers at level " + level + ": " + taggerFile)

      val taggerText = using(Source.fromFile(taggerFile)) { source =>
        source.getLines.mkString("\n")
      }

      level -> TaggerEntry(taggerFile.getName, taggerText)
    }
    
    IntMap.empty[TaggerEntry] ++ levels
  }

  private def load[S <: Sentence](basePath: File, lines: Iterator[String]): Cascade[S] = {
    def loadTaggers(text: String) = {
      // parse taggers
      val rules = new RuleParser[S].parse(text).get

      Taggers.fromRules(rules)
    }

    System.err.println("Loading cascade: " + basePath)

    // paths inside are either absolute or relative to the cascade definition file
    def makeFile(path: String) = {
      val file = new File(path)
      if (file.isAbsolute) file
      else new File(basePath, path)
    }

    var cascade = new Cascade[S]()

    // Iterate over the level definitions, load the tagger files,
    // and add them to the cascade.
    val levels = for (
      line <- lines map (_.trim) if !line.isEmpty
    ) {

      // A line is composed of a level number and a tagger file path
      val (level, taggerFile) = line.split("\t") match {
        case Array(levelString, taggerFilePath) =>
          (levelString.toInt, makeFile(taggerFilePath))
        case _ => throw new MatchError("Could not understand cascade line: " + line)
      }

      System.err.println("Loading taggers at level " + level + ": " + taggerFile)

      val taggers = using(Source.fromFile(taggerFile)) { source =>
        loadTaggers(source.getLines.mkString("\n"))
      }

      cascade = cascade.plus(level, taggers)
    }

    System.err.println("Done loading cascade.")
    System.err.println()

    cascade
  }
}
