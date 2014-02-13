package edu.knowitall.taggers

import edu.knowitall.tool.typer.Type

import scala.util.parsing.combinator.RegexParsers
import scala.util.{ Try, Success, Failure }
import java.io.Reader
import java.io.StringReader

object Extractor {
  def findAlignedTypes(typ: Type, candidates: Iterable[Type]): Iterable[Type] = {
    candidates filter (_.tokenInterval == typ.tokenInterval)
  }

  def findAlignedTypesWithName(typ: Type, name: String, candidates: Iterable[Type]) = {
    findAlignedTypes(typ, candidates) filter (_.name == name)
  }

  def findSubtypes(typ: Type, candidates: Iterable[Type]): Iterable[LinkedType] = {
    candidates collect {
      case candidate: LinkedType
        if candidate.link == Some(typ) =>
          candidate
    }
  }

  def findSubtypesWithName(typ: Type, subtypeName: String, candidates: Iterable[Type]): Iterable[NamedGroupType] = {
    candidates collect {
      case candidate: NamedGroupType
        if candidate.link == Some(typ)
        && candidate.groupName == subtypeName =>
          candidate
    }
  }

  sealed abstract class BuilderPart {
    def stringFrom(variable: String, typ: Type, types: Iterable[Type]): String
  }

  case class SimpleBuilderPart(string: String) extends BuilderPart {
    override def toString = string
    override def stringFrom(variable: String, typ: Type, types: Iterable[Type]) = string
  }

  case class SubstitutionBuilderPart(base: String, aligns: Seq[AlignExpr], fallback: Option[String]) extends BuilderPart {
    override def toString = "${" + base + (aligns map (_.toString)).mkString("") + fallback.map("|" + _).getOrElse("") + "}"
    val Subtype = "(\\w+).(\\w+)".r
    override def stringFrom(variable: String, typ: Type, allTypes: Iterable[Type]): String = {
      try {
        // Find the starting type for this subtitution.
        // It will be either a subtype of typ or typ itself.
        val startingType = base match {
          case Subtype(variableUsage, subtype) =>
            require(variableUsage == variable, "Unbounded variable: " + variableUsage)
            val subtypes = Extractor.findSubtypesWithName(typ, subtype, allTypes)
            require(subtypes.size > 0, s"No subtype type '$subtype' found for: $typ")
            require(subtypes.size <= 1, s"Multiple subtype types '$subtype' found for: $typ")
            subtypes.head
          case `variable` => typ
          case v => throw new IllegalArgumentException("Unbounded variable: " + v)
        }

        // For each iteration, we want to find a type named "align" that has the
        // same interval as our current type.  Then we want to find the named
        // subtype specified.
        var currentType = startingType
        for (AlignExpr(align, subtype) <- aligns) {
          val aligned = Extractor.findAlignedTypesWithName(currentType, align, allTypes)
          require(aligned.size > 0, s"No aligned type '$align' found for: $currentType")
          require(aligned.size <= 1, s"Multiple aligned types '$align' found for: $currentType")

          val subtyped = Extractor.findSubtypesWithName(aligned.head, subtype, allTypes)
          require(subtyped.size > 0, s"No subtype type '$subtype' found for: $aligned")
          require(subtyped.size <= 1, s"Multiple subtype types '$subtype' found for: $aligned")

          currentType = subtyped.head
        }

        currentType.text
      } catch {
        case e if fallback.isDefined=> fallback.get
      }
    }
  }

  case class AlignExpr(align: String, subtype: String)
}

class ExtractorParser extends RegexParsers {
  import Extractor._

  val token = "\\w+".r

  val baseType = token
  val typeWithSubtype = "\\w+\\.\\w+".r

  val typ = typeWithSubtype | baseType

  val alignExpr = "->" ~> baseType ~ "." ~ baseType ^^ { case alignedType ~ "." ~ subType =>
    AlignExpr(alignedType, subType)
  }

  val substitutionWithFallback: Parser[BuilderPart] = "${" ~> typ ~ (alignExpr*) ~ "|" ~ "[^}]+".r <~ "}" ^^ { case base ~ exprs ~ "|" ~ fallback =>
    SubstitutionBuilderPart(base, exprs, Some(fallback))
  }

  val substitutionWithoutFallback: Parser[BuilderPart] = "${" ~> typ ~ (alignExpr*) <~ "}" ^^ { case base ~ exprs =>
    SubstitutionBuilderPart(base, exprs, None)
  }

  val substitution = substitutionWithFallback | substitutionWithoutFallback

  val specPart: Parser[List[BuilderPart]] = (opt(substitution) ~ "[^$]+".r ~ rep(substitution)) ^^ { case subOpt ~ string ~ subRep =>
    List(SimpleBuilderPart(string)) ++ subOpt ++ subRep
  } | substitution ^^ { case sub => List(sub) }

  val spec: Parser[Extractor] = token ~ "\\s*:\\s*".r ~ token ~ "\\s*=>\\s*".r ~ rep(specPart) ^^ { case variable ~ _ ~ foreach ~ _ ~ parts =>
    new Extractor(variable, foreach, parts.flatten)
  }

  def parse(string: String): Try[Extractor] = this.parse(new StringReader(string))
  def parse(reader: Reader): Try[Extractor] = parseAll(spec, reader) match {
    case this.Success(ast, _) => scala.util.Success(ast)
    case this.NoSuccess(err, next) => Try(throw new IllegalArgumentException("failed to parse rule " +
      "(line " + next.pos.line + ", column " + next.pos.column + "):\n" +
      err + "\n" +
      next.pos.longString))
  }
}

case class Extractor(variable: String, targetType: String, parts: Seq[Extractor.BuilderPart]) {
  override def toString = {
    variable + ": " + targetType + " => " + (parts map (_.toString)).mkString("")
  }
  def apply(types: Iterable[Type]): Seq[String] = {
    (for (
      typ <- types
      if typ.name == targetType
    ) yield {
      (parts map (_.stringFrom(variable, typ, types))).mkString("")
    })(scala.collection.breakOut)
  }
}
