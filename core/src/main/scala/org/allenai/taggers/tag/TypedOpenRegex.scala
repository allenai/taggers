package org.allenai.taggers.tag

import edu.knowitall.openregex
import org.allenai.taggers.Cascade
import org.allenai.taggers.constraint.Constraint
import org.allenai.taggers.pattern.PatternBuilder
import org.allenai.taggers.pattern.TypedToken
import org.allenai.taggers.TypeHelper
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.nlpstack.typer.Type
import edu.washington.cs.knowitall.regex.Expression.NamedGroup

import scala.util.matching.Regex

class TypedOpenRegex(name: String, expression: String)
    extends OpenRegex(name, TypedOpenRegex.expandWholeTypeSyntax(expression)) {
  val targetTypes: Set[String] = {
    val names = for (data <- TypedOpenRegex.wholeTypeSyntaxPattern.findAllIn(expression).matchData) yield {
      data.group(1)
    }

    names.toSet
  }

  /** The constructor used by reflection.
    *
    * Multiple lines are collapsed to create a single expression.
    */
  def this(name: String, expressions: Seq[String]) = {
    this(name, expressions.mkString(" "))
  }

  override def typecheck(definedTypes: Set[String]) = {
    def baseType(t: String) = t takeWhile (_ != '.')
    targetTypes find { t => !(definedTypes contains baseType(t)) } match {
      case Some(t) => throw new IllegalArgumentException(s"Unknown type $t in $name: $expression")
      case None =>
    }
  }
}

object TypedOpenRegex {
  // Match an @-sign followed by a type name.
  val wholeTypeSyntaxPattern = new Regex("@(\\w+(?:\\.\\w+)?)(?![^<]*>)")

  private def expandWholeTypeSyntax(str: String): String = {
    wholeTypeSyntaxPattern.replaceAllIn(str, m => {
      "(?:(?:<typeStart='" + m.group(1) + "' & typeEnd='" + m.group(1) + "'>) | (?: <typeStart='" + m.group(1) + "' & !typeEnd='" + m.group(1) + "'> (?: <typeCont='" + m.group(1) + "' & !typeEnd='" + m.group(1) + "'>)* <typeEnd='" + m.group(1) + "'>))"
    })
  }
}
