package edu.knowitall.taggers.pattern

import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.openregex
import java.util.regex.Pattern
import edu.washington.cs.knowitall.logic.LogicExpression
import edu.knowitall.collection.immutable.Interval
import edu.washington.cs.knowitall.regex.Expression
import edu.washington.cs.knowitall.logic
import com.google.common.base.{ Function => GuavaFunction }
import org.apache.commons.lang3.StringEscapeUtils

object PatternBuilder {
  type Token = TypedToken

  implicit def logicArgFromFunction[T](f: T => Boolean) = new logic.Expression.Arg[T] {
    override def apply(token: T) = f(token)
  }

  implicit def guavaFromFunction[A, B](f: A => B) = new GuavaFunction[A, B] {
    override def apply(a: A) = f(a)
  }

  def compile(pattern: String) =
    openregex.Pattern.compile(pattern, (expression: String) => {
      // mostly copied from scala.util.parsing.combinator.JavaTokenParser
      val doubleQuoteStringLiteralRegex = ("\"" + """((?:[^"\p{Cntrl}\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*)""" + "\"").r
      val singleQuoteStringLiteralRegex = ("'" + """([^']*)""" + "'").r
      val regexLiteralRegex = ("/" + """([^/]*(?:(?:\\)|(?:\/))?)""" + "/").r

      val baseExpr = new Expression.BaseExpression[Token](expression) {
        val deserializeToken: String => (Token => Boolean) = (argument: String) => {
          val Array(base, value) = argument.split("=").map(_.trim)

          val field = base match {
            case "string" => Fields.StringField
            case "lemma" => Fields.LemmaField
            case "pos" => Fields.PostagField
            case "chunk" => Fields.ChunkField

            case "type" => Fields.TypeField
            case "typeStart" => Fields.TypeStartField
            case "typeCont" => Fields.TypeContField
            case "typeEnd" => Fields.TypeEndField

            case x => throw new MatchError("Unknown field: " + x)
          }

          value match {
            case doubleQuoteStringLiteralRegex(string) =>
              val unescapedString = StringEscapeUtils.unescapeJava(string)
              new Expressions.StringExpression(field, unescapedString)
            case singleQuoteStringLiteralRegex(string) =>
              new Expressions.StringExpression(field, string)
            case regexLiteralRegex(string) =>
              val unescapedString = string.replaceAll("""\/""", "/").replaceAll("""\\""", """\""")
              new Expressions.RegularExpression(field, string)
            case _ => throw new IllegalArgumentException("Value not enclosed in quote (\") or (') or (/): " + argument)
          }
        }

        val logic: LogicExpression[Token] =
          LogicExpression.compile(expression, deserializeToken andThen logicArgFromFunction[Token])

        override def apply(token: Token): Boolean = logic.apply(token)
      }

      baseExpr: Expression.BaseExpression[Token]
    })

  def intervalFromGroup(group: openregex.Pattern.Group[_]): Interval = {
    val interval = group.interval

    if (interval.start == -1 || interval.end == -1) {
      Interval.empty
    } else {
      interval
    }
  }
}