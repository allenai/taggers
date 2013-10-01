package edu.knowitall.taggers.tag

import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.taggers.pattern.TypedToken
import edu.knowitall.openregex
import edu.knowitall.taggers.pattern.PatternBuilder
import edu.washington.cs.knowitall.regex.Expression.NamedGroup
import edu.knowitall.taggers.TypeHelper
import scala.util.matching.Regex
import edu.knowitall.taggers.TaggerCollection


class TypePatternTagger(name: String, typePatternExpressions: Seq[String]) extends PatternTagger(name, typePatternExpressions map TypePatternTagger.expandWholeTypeSyntax)

object TypePatternTagger{
  
    val wholeTypeSyntaxPattern = new Regex("@(\\w+)(?![^<]*>)")

    private def expandWholeTypeSyntax(str: String) :String = {
    wholeTypeSyntaxPattern.replaceAllIn(str, m => {
      "<typeStart='"+m.group(1)+"'> <typeCont='"+m.group(1)+"'>*"
    })
  }
  
}