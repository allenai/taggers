package edu.knowitall.taggers

import edu.knowitall.common.Resource
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.rule._
import edu.knowitall.taggers.tag._
import edu.knowitall.tool.typer.Type

import java.io.File
import java.io.Reader
import scala.io.Source

case class Level[-S <: Tagger.Sentence](imports: Seq[Import], taggers: Seq[Tagger[S]]) {
  def this(taggers: Seq[Tagger[S]]) = this(Seq.empty, taggers)

  /** Check to make sure the imported types are defined earlier
    * in the Cascade.  This is not strictly necessary, but it will
    * help catch errors earlier. */
  def typecheck(definedTypeNames: Set[String]) = {
    for (im <- imports) {
      // Strip out any text after a period
      val startImportName = im.name.takeWhile(_ != '.')
      require(definedTypeNames contains startImportName, "Imported type undefined: " + startImportName)
    }
  }

  /** Filter to keep only the types that were explicitly imported. */
  def filterTypes(types: Seq[Type]) = {
    types filter (t => imports exists (_.name == t.name))
  }

  /** Filter to keep only the types that were explicitly imported. */
  def filterTypes(types: Set[Type]) = {
    types filter (t => imports exists (_.name == t.name))
  }

  def apply(sentence: S, types: Seq[Type]): Seq[Type] = {
    sentence.reset()
    for (typ <- types) sentence.consume(typ)

    val importNames = imports map (_.name)

    // filter all the types by those imported
    val availableTypes = filterTypes(types)

    var levelTypes = Seq.empty[Type]
    val consumedIndices = (availableTypes map(_.tokenInterval)).flatten

    for (tagger <- taggers) yield {
      val allTypes = availableTypes ++ levelTypes
      levelTypes = levelTypes ++ tagger(sentence, allTypes)
    }

    levelTypes
  }
}

object Level {
  def fromFile[S <: Tagger.Sentence](file: File): Level[S] = {
    Resource.using(Source.fromFile(file)) { input =>
      this.fromReader[S](input.bufferedReader)
    }
  }

  def fromReader[S <: Tagger.Sentence](reader: Reader): Level[S] = {
    this.fromParsed(new RuleParser[S].parse(reader).get)
  }

  def fromString[S <: Tagger.Sentence](string: String): Level[S] = {
    this.fromParsed(new RuleParser[S].parse(string).get)
  }

  def fromParsed[S <: Tagger.Sentence](parsed: ParsedLevel[S]): Level[S] = {
    var definitions: Seq[DefinitionRule[S]] = Seq.empty
    var taggers: Seq[Tagger[S]] = Seq.empty

    for (rule <- parsed.rules) {
      rule match {
        case defn: DefinitionRule[S] => definitions = definitions :+ defn
        case rule: TaggerRule[S] => taggers = taggers :+ rule.instantiate(definitions)
      }
    }

    Level(parsed.imports, taggers)
  }
}
