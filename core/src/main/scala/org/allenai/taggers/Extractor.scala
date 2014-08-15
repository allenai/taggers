package org.allenai.taggers

import org.allenai.nlpstack.core.typer.Type

import scala.util.parsing.combinator.RegexParsers
import scala.util.{Failure, Success, Try}
import java.io.{Reader, StringReader}

/** An extractor definition, which builds an extraction string from a set of types.
  * It will be iteratively applied to all of the target types.
  *
  * @param  variable  the name of the variable to bind to
  * @param  targetType  the type to apply this extractor to
  * @param  parts  the logic to build up a string from a type
  */
case class Extractor(variable: String, targetType: String, parts: Seq[Extractor.BuilderPart]) {
  override def toString = {
    variable + ": " + targetType + " => " + (parts map (_.toString)).mkString("")
  }

  def typecheck(definedTypes: Set[String]) = {
    require(definedTypes contains this.targetType,
      "Extractor depends on undefined type: " + this.targetType)
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

object Extractor {
  /** Find types that have the same interval as typ.
    *
    * @param  candidates  the types to look through
    * @param  typ  the type to align with
    */
  def findAlignedTypes(candidates: Iterable[Type])(typ: Type): Iterable[Type] = {
    candidates filter (_.tokenInterval == typ.tokenInterval)
  }

  /** Find types that have the same interval as typ.
    *
    * @param  candidates  the types to look through
    * @param  typ  the type to align with
    * @param  name  a name filter for the aligned types
    */
  def findAlignedTypesWithName(candidates: Iterable[Type])(typ: Type, name: String) = {
    findAlignedTypes(candidates)(typ) filter (_.name == name)
  }

  /** Find subtypes of the specified type.
    *
    * @param  candidates  the types to look through
    * @param  typ  the parent type
    */
  def findSubtypes(candidates: Iterable[Type])(typ: Type): Iterable[LinkedType] = {
    candidates collect {
      case candidate: LinkedType
        if candidate.link == Some(typ) =>
          candidate
    }
  }

  /** Find subtypes of the specified type.
    *
    * @param  candidates  the types to look through
    * @param  typ  the parent type
    * @param  name  a name filter for the subtypes
    */
  def findSubtypesWithName(candidates: Iterable[Type])(typ: Type, subtypeName: String): Iterable[NamedGroupType] = {
    candidates collect {
      case candidate: NamedGroupType
        if candidate.link == Some(typ)
        && candidate.groupName == subtypeName =>
          candidate
    }
  }

  /** A component in an expression that is evaluated against a set of types
    * to create an extraction string. */
  sealed abstract class BuilderPart {
    /** Create a string with the supplied context. */
    def stringFrom(variable: String, typ: Type, types: Iterable[Type]): String
  }

  /** A trivial builder component that is always the specified string. */
  case class SimpleBuilderPart(string: String) extends BuilderPart {
    override def toString = string
    override def stringFrom(variable: String, typ: Type, types: Iterable[Type]) = string
  }

  /** A piece of a substitution expression (i.e. ${ ... })
    *
    * @param  base  either the bound variable or a subtype
    * @param  aligns  rules to move to other types from the base type
    * @param  fallback  a string used if the expression fails
    */
  abstract class SubstitutionPart {
    def stringFrom(variable: String, typ: Type, allTypes: Iterable[Type]): Try[String]
  }
  case class SubstitutionString(string: String) extends SubstitutionPart {
    override def toString = "'" + string + "'"
    override def stringFrom(variable: String, typ: Type, allTypes: Iterable[Type]): Try[String] =
      Success(string)
  }
  case class SubstitutionExpr(base: String, aligns: Seq[AlignExpr]) extends SubstitutionPart {
    private val Subtype = "(\\w+).(\\w+)".r
    override def toString = base + (aligns map (_.toString)).mkString("")
    override def stringFrom(variable: String, typ: Type, allTypes: Iterable[Type]): Try[String] = {
      Try {
        // Find the starting type for this subtitution.
        // It will be either a subtype of typ or typ itself.
        val startingType = base match {
          case Subtype(variableUsage, subtype) =>
            require(variableUsage == variable, "Unbounded variable: " + variableUsage)
            val subtypes = Extractor.findSubtypesWithName(allTypes)(typ, subtype)
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
          val aligned = Extractor.findAlignedTypesWithName(allTypes)(currentType, align)
          require(aligned.size > 0, s"No aligned type '$align' found for: $currentType")
          require(aligned.size <= 1, s"Multiple aligned types '$align' found for: $currentType")

          val subtyped = Extractor.findSubtypesWithName(allTypes)(aligned.head, subtype)
          require(subtyped.size > 0, s"No subtype type '$subtype' found for: $aligned")
          require(subtyped.size <= 1, s"Multiple subtype types '$subtype' found for: $aligned")

          currentType = subtyped.head
        }

        currentType.text
      }
    }
  }

  /** A builder component that is constructed through a type expression.
    *
    * @param  attempts  expression to try, one after another
    */
  case class SubstitutionBuilderPart(attempts: Seq[SubstitutionPart]) extends BuilderPart {
    override def toString = "${" + attempts.mkString("|") + "}"

    def stringFrom(variable: String, typ: Type, types: Iterable[Type]): String = {
      var lastError: Throwable = null
      for (attempt <- attempts) {
        attempt.stringFrom(variable, typ, types) match {
          case Success(string) => return string
          case Failure(e) => lastError = e
        }
      }

      throw lastError
    }
  }

  /** An expression that finds a type aligned with the specified type, and
    * then takes the specified subtype of the aligned type.
    *
    * @param  align  the name of the type to align with
    * @param  subtype  the subtype of the aligned type to use
    */
  case class AlignExpr(align: String, subtype: String) {
    override def toString = s"->$align.$subtype"
  }
}

/** A parser combinator to parse Extractor definitions.
  * For examples, see ExtractorSpec. */
class ExtractorParser extends RegexParsers {
  import org.allenai.taggers.Extractor._

  override def skipWhitespace = false

  val token = "\\w+".r

  val baseType = token
  val typeWithSubtype = "\\w+\\.\\w+".r

  val typ = typeWithSubtype | baseType

  val alignExpr = "->" ~> baseType ~ "." ~ baseType ^^ { case alignedType ~ "." ~ subType =>
    AlignExpr(alignedType, subType)
  }

  val substitutionString = "\'" ~> "[^']*".r <~ "'" ^^ { case string =>
    SubstitutionString(string)
  }

  val substitutionExpr = typ ~ (alignExpr*) ^^ { case base ~ exprs =>
    SubstitutionExpr(base, exprs)
  }

  val substitutionPart: Parser[SubstitutionPart] = substitutionString | substitutionExpr

  val substitution: Parser[BuilderPart] = "${" ~> substitutionPart ~ rep("|" ~> substitutionPart) <~ "}" ^^ { case head ~ tail =>
    SubstitutionBuilderPart(head +: tail)
  }

  val specPart: Parser[List[BuilderPart]] = (opt(substitution) ~ "[^$]+".r ~ rep(substitution)) ^^ { case subOpt ~ string ~ subRep =>
    List(SimpleBuilderPart(string)) ++ subOpt ++ subRep
  } | substitution ^^ { case sub => List(sub) }

  val spec: Parser[Extractor] = token ~ "\\s*:\\s*".r ~ token ~ "\\s*=>\\s*".r ~ rep(specPart) ^^ { case variable ~ _ ~ foreach ~ _ ~ parts =>
    new Extractor(variable, foreach, parts.flatten)
  }

  def parse(string: String): Try[Extractor] = this.parse(new StringReader(string))
  def parse(reader: Reader): Try[Extractor] = parseAll(spec, reader) match {
    case this.Success(ast, _) => scala.util.Success(ast)
    case this.NoSuccess(err, next) => Try(throw new IllegalArgumentException("failed to parse tagger definition " +
      "(line " + next.pos.line + ", column " + next.pos.column + "):\n" +
      err + "\n" +
      next.pos.longString))
  }
}
