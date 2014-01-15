package edu.knowitall.taggers.pattern

import edu.knowitall.taggers.pattern.Fields.Field
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized

import java.util.regex.Pattern

/** A collection of matchers against a token. */
object Expressions {
  abstract class Expression extends Function[PatternBuilder.Token, Boolean] {
    def field: Field

    def applies(token: PatternBuilder.Token): Boolean =
      !token.consumed || field.isInstanceOf[Fields.AbstractTypeField]

    /** This method is to be overridden. */
    protected def matches(token: PatternBuilder.Token): Boolean

    /** Use overridden logic and common logic. */
    final def apply(token: PatternBuilder.Token): Boolean = {
      applies(token) && matches(token)
    }
  }

  case class RegularExpression(field: Field, pattern: Pattern) extends Expression {
    def this(field: Field, pattern: String, flags: Int) = this(field, Pattern.compile(pattern, flags))
    def this(field: Field, pattern: String) = this(field, pattern, 0)

    def matches(token: PatternBuilder.Token) = {
      field(token).exists(field => pattern.matcher(field).matches())
    }
  }

  case class StringExpression(field: Field, string: String) extends Expression {
    def matches(token: PatternBuilder.Token) = {
      field(token).exists(_ == string)
    }
  }

  case class StringCIExpression(field: Field, string: String) extends Expression {
    def matches(token: PatternBuilder.Token) = {
      field(token).exists(_ equalsIgnoreCase string)
    }
  }
}
