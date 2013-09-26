package edu.knowitall.taggers.pattern

import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import java.util.regex.Pattern
import edu.knowitall.taggers.pattern.Fields.Field

object Expressions {
  abstract class Expression extends Function[PatternBuilder.Token, Boolean] {
    def field: Field
  }
  case class RegularExpression(field: Field, pattern: Pattern) extends Expression {
    def this(field: Field, pattern: String, flags: Int) = this(field, Pattern.compile(pattern, flags))
    def this(field: Field, pattern: String) = this(field, pattern, 0)

    def apply(token: PatternBuilder.Token) = {
      field(token).exists(field => pattern.matcher(field).matches())
    }
  }
  case class StringExpression(field: Field, string: String) extends Expression {
    def apply(token: PatternBuilder.Token) = {
      field(token).exists(_ == string)
    }
  }
}