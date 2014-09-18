package org.allenai.taggers

import java.io.File

import scala.collection.immutable
import scala.io.Source
import scala.util.Failure
import scala.util.Success

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.typer.Type
import org.allenai.taggers.tag.Tagger
import org.slf4j.LoggerFactory

import edu.knowitall.common.Resource.using

/** Represents a sequence of taggers applied in order.
  * After each level of taggers, OpenRegexs can only use
  * type information from previous levels.
  *
  * @param  levels  stores the taggers applied on each level
  */
case class Cascade[-S <: Tagger.Sentence](name: String, levels: Seq[Level[S]] = Seq.empty, extractors: Seq[Extractor] = Seq.empty) {
  lazy val chunker = new OpenNlpChunker()

  // Make sure all the imports are valid.
  // Make sure all extractors are for defined types.
  typecheck()

  /** Convenience constructor to make a Cascade with a single level.
    * This is primarily used by the test cases.  The resulting cascade
    * will be unnamed. */
  private[taggers] def this(level: Level[S]) = this("unnamed", Seq(level), Seq.empty)

  /** Convenience constructor to make a Cascade with a multiple levels.
    * This is primarily used by the test cases.  The resulting cascade
    * will be unnamed. */
  private[taggers] def this(levels: Seq[Level[S]]) = this("unnamed", levels, Seq.empty)

  def typecheck() = {
    var definedTypes = Set.empty[String]
    for (level <- levels) {
      try {
        level.typecheck(definedTypes)
      }
      catch {
        case e: Exception =>
          throw new IllegalArgumentException("Typechecking error on level: " + level.name, e)
      }

      definedTypes ++= (level.taggers.iterator map (_.name)).toSet
    }

    for (extractor <- extractors) {
      extractor.typecheck(definedTypes)
    }
  }

  /** Apply the cascade to a sentence.
    *
    * @return  the found types and extractions
    */
  def apply(sentence: S): (Seq[Type], Seq[String]) = {
    var previousTypes = Seq.empty[Type]
    var previousLevelTypes = Seq.empty[Type]
    for (level <- levels) {
      val levelTypes = level.apply(sentence, previousTypes)

      previousTypes ++= levelTypes
      previousLevelTypes = levelTypes
    }

    (previousLevelTypes, this.extract(previousTypes).values.flatten.toSeq)
  }

  /** Apply the extractors to the types yielded by this cascade.
    *
    * @return  the found extractions
    */
  def extract(types: Iterable[Type]): Map[String, Seq[String]] = {
    (for (extractor <- extractors) yield {
      extractor.toString -> extractor(types)
    })(scala.collection.breakOut)
  }

  /** Apply the cascade to a sentence, keeping types found at all levels.
    *
    * @return  the found types at each level
    */
  def levelTypes(sentence: S): immutable.ListMap[String, Seq[Type]] = {
    var previousTypes = Seq.empty[Type]
    var result = immutable.ListMap.empty[String, Seq[Type]]
    var definedTypes = Set.empty[String]
    for (level <- levels) {
      definedTypes ++= (level.taggers.iterator map (_.name)).toSet

      val levelTags = level.apply(sentence, previousTypes)

      result += level.name -> levelTags
      previousTypes ++= levelTags
    }

    result
  }
}

object Cascade {
  val logger = LoggerFactory.getLogger(this.getClass)

  /** LevelDefinition models a file with a level definition.
    *
    * @param  filename  the name of the file
    * @param  text  the contents of the file
    */
  case class LevelDefinition(filename: String, text: String)

  // load a cascade definition file
  def load[S <: Tagger.Sentence](cascadeFile: File, cascadeName: String): Cascade[S] = {
    using(Source.fromFile(cascadeFile)) { source =>
      load(cascadeFile.getParentFile, source, cascadeName)
    }
  }

  def load[S <: Tagger.Sentence](basePath: File, cascadeSource: Source, cascadeName: String): Cascade[S] = {
    logger.info("Loading cascade definition: " + basePath)

    var levels = Seq.empty[Level[S]]
    val (taggerEntries, extractors) = partialLoad(basePath, cascadeSource.getLines)
    for (LevelDefinition(filename, text) <- taggerEntries) {
      logger.info("Parsing taggers from: " + filename)
      levels = levels :+ Level.fromString(filename, text)
    }

    logger.info("Done loading cascade.")

    Cascade(cascadeName, levels, extractors)
  }

  def partialLoad(cascadeFile: File): (Seq[LevelDefinition], Seq[Extractor]) = {
    using(Source.fromFile(cascadeFile)) { source =>
      partialLoad(cascadeFile.getParentFile, source)
    }
  }

  def partialLoad(basePath: File, cascadeSource: Source): (Seq[LevelDefinition], Seq[Extractor]) = {
    partialLoad(basePath, cascadeSource.getLines)
  }

  private def partialLoad(basePath: File, lines: Iterator[String]): (Seq[LevelDefinition], Seq[Extractor]) = {
    // paths inside are either absolute or relative to the cascade definition file
    def makeFile(path: String) = {
      val file = new File(path)
      if (file.isAbsolute) file
      else new File(basePath, path)
    }

    val extractorParser = new ExtractorParser()

    // Iterate over the level definitions, load the tagger files,
    // and add them to the cascade.
    var levels = Seq.empty[LevelDefinition]
    var extractors = Seq.empty[Extractor]
    for {
      line <- lines map (_.trim) if !line.isEmpty
    } {
      // Determine if this line contains a tagger file or an extractor.
      extractorParser.parse(line) match {
        case Success(extractor) =>
          logger.debug("Adding extractor: " + extractor.toString)
          extractors :+= extractor
        case Failure(e) =>
          // A line is composed of a tagger file path
          val taggerFilePath = line
          val taggerFile = makeFile(taggerFilePath)

          logger.debug("Loading taggers from: " + taggerFile)

          val taggerText = using(Source.fromFile(taggerFile)) { source =>
            source.getLines.mkString("\n")
          }

          levels :+= LevelDefinition(taggerFile.getName, taggerText)
      }
    }

    (levels, extractors)
  }
}
