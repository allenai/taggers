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
import scala.util.{ Try, Success, Failure }

/** Represents a sequence of taggers applied in order.
  * After each level of taggers, PatternTaggers can only use
  * type information from previous levels.
  *
  * @param  levels  stores the taggers applied on each level
  */
case class Cascade[-S <: Sentence](levels: Seq[Level[S]] = Seq.empty, extractors: Seq[Extractor] = Seq.empty) {
  lazy val chunker = new OpenNlpChunker()

  // Make sure all the imports are valid.
  // Make sure all extractors are for defined types.
  {
    var definedTypes = Set.empty[String]
    for ((level, i) <- levels.zipWithIndex) {
      try {
        level.typecheck(definedTypes)
      } catch {
        case e: Exception =>
          throw new IllegalArgumentException("Import error on level: " + i, e)
      }
      definedTypes ++= level.taggers.iterator.map(_.name)
    }

    for (extractor <- extractors) {
      require(definedTypes contains extractor.targetType,
        "Extractor depends on undefined type: " + extractor.targetType)
    }
  }

  /** Convenience constructor to make a Cascade with a single Level. */
  def this(level: Level[S]) = this(Seq(level), Seq.empty)

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

  /** Apply the extractors to the types yielded by this cascade.
    *
    * @returns  the found extractions
    */
  def extract(types: Iterable[Type]): Map[String, Seq[String]] = {
    (for (extractor <- extractors) yield {
      extractor.toString -> extractor(types)
    })(scala.collection.breakOut)
  }

  /** Apply the cascade to a sentence, keeping types found at all levels.
    *
    * @returns  the found types at each level
    */
  def levelTypes(sentence: S): immutable.ListMap[Int, Seq[Type]] = {
    var previousTypes = Seq.empty[Type]
    var result = immutable.ListMap.empty[Int, Seq[Type]]
    var definedTypes = Set.empty[String]
    for ((level, index) <- levels.zipWithIndex) {
      level.typecheck(definedTypes)
      definedTypes ++= (level.taggers.iterator map (_.name)).toSet

      val levelTags = level.apply(sentence, previousTypes)

      result += index -> levelTags
      previousTypes ++= levelTags
    }

    result
  }
}

object Cascade {
  case class RawLevel(filename: String, text: String)

  // load a cascade definition file
  def load[S <: Sentence](cascadeFile: File): Cascade[S] = {
    using(Source.fromFile(cascadeFile)) { source =>
      load(cascadeFile.getParentFile, source)
    }
  }

  def load[S <: Sentence](basePath: File, cascadeSource: Source): Cascade[S] = {
    System.err.println("Loading cascade definition: " + basePath)

    var levels = Seq.empty[Level[S]]
    val (taggerEntries, extractors) = partialLoad(basePath, cascadeSource.getLines)
    for (RawLevel(filename, text) <- taggerEntries) {
      System.err.println("Parsing taggers from: " + filename)
      levels = levels :+ Level.fromString(text)
    }

    System.err.println("Done loading cascade.")
    System.err.println()

    Cascade(levels, extractors)
  }

  def partialLoad(cascadeFile: File): (Seq[RawLevel], Seq[Extractor]) = {
    using(Source.fromFile(cascadeFile)) { source =>
      partialLoad(cascadeFile.getParentFile, source)
    }
  }

  def partialLoad(basePath: File, cascadeSource: Source): (Seq[RawLevel], Seq[Extractor]) = {
    partialLoad(basePath, cascadeSource.getLines)
  }

  private def partialLoad(basePath: File, lines: Iterator[String]): (Seq[RawLevel], Seq[Extractor]) = {
    // paths inside are either absolute or relative to the cascade definition file
    def makeFile(path: String) = {
      val file = new File(path)
      if (file.isAbsolute) file
      else new File(basePath, path)
    }

    val extractorParser = new ExtractorParser()

    // Iterate over the level definitions, load the tagger files,
    // and add them to the cascade.
    var levels = Seq.empty[RawLevel]
    var extractors = Seq.empty[Extractor]
    for {
      line <- lines map (_.trim) if !line.isEmpty
    } {
      // Determine if this line contains a tagger file or an extractor.
      extractorParser.parse(line) match {
        case Success(extractor) =>
          System.err.println("Adding extractor: " + extractor.toString)
          extractors :+= extractor
        case Failure(e) =>
          // A line is composed of a tagger file path
          val taggerFilePath = line
          val taggerFile = makeFile(taggerFilePath)

          System.err.println("Loading taggers from: " + taggerFile)

          val taggerText = using(Source.fromFile(taggerFile)) { source =>
            source.getLines.mkString("\n")
          }

          levels :+= RawLevel(taggerFile.getName, taggerText)
      }
    }

    (levels, extractors)
  }
}
