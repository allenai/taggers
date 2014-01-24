package edu.knowitall.taggers.tag

import edu.knowitall.openregex
import edu.knowitall.taggers.Cascade
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.pattern.PatternBuilder
import edu.knowitall.taggers.pattern.TypedToken
import edu.knowitall.taggers.TypeHelper
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.typer.Type
import edu.washington.cs.knowitall.regex.Expression.NamedGroup

import scala.util.matching.Regex

class TypePatternTagger(name: String, expression: String)
    extends PatternTagger(name, TypePatternTagger.expandWholeTypeSyntax(expression)) {
  /** The constructor used by reflection.
    *
    * Multiple lines are collapsed to create a single expression.
    */
  def this(name: String, expressions: Seq[String]) = {
    this(name, expressions.mkString(" "))
  }
}

object TypePatternTagger {
  val wholeTypeSyntaxPattern = new Regex("@(\\w+)(?![^<]*>)")

  private def expandWholeTypeSyntax(str: String): String = {
    wholeTypeSyntaxPattern.replaceAllIn(str, m => {
      "(?:(?:<typeStart='" + m.group(1) + "' & typeEnd='" + m.group(1) + "'>) | (?: <typeStart='" + m.group(1) + "' & !typeEnd='" + m.group(1) + "'> <typeCont='" + m.group(1) + "'>* <typeEnd='" + m.group(1) + "'>))"
    })
  }
}
