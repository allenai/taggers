package edu.knowitall.taggers.pattern

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.openregex
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.washington.cs.knowitall.logic
import edu.washington.cs.knowitall.logic.LogicExpression
import edu.washington.cs.knowitall.regex.Expression

import com.google.common.base.{ Function => GuavaFunction }
import org.apache.commons.lang3.StringEscapeUtils

import java.util.regex.Pattern

object PatternBuilder {
  type Token = TypedToken

  implicit def logicArgFromFunction[T](f: T => Boolean) = new logic.Expression.Arg[T] {
    override def apply(token: T) = f(token)
  }

  implicit def guavaFromFunction[A, B](f: A => B) = new GuavaFunction[A, B] {
    override def apply(a: A) = f(a)
  }

  // mostly copied from scala.util.parsing.combinator.JavaTokenParser
  val doubleQuoteStringLiteralPattern = "\"" + """((?:[^"\p{Cntrl}\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*)""" + "\""
  val doubleQuoteStringLiteralRegex = doubleQuoteStringLiteralPattern.r

  val singleQuoteStringLiteralPattern = "'" + """([^']*)""" + "'"
  val singleQuoteStringLiteralRegex = singleQuoteStringLiteralPattern.r

  val caseInsensitiveDoubleQuoteStringLiteralRegex = ("i" + doubleQuoteStringLiteralPattern).r
  val caseInsensitiveSingleQuoteStringLiteralRegex = ("i" + singleQuoteStringLiteralPattern).r

  val regexLiteralRegex = ("/" + """((?:[^/\\]*(?:\\)*(?:\\/)*)*)""" + "/").r

  /** This class compiles regular expressions over the tokens in a sentence
    * into an NFA. There is a lot of redundancy in their expressiveness. This
    * is largely because it supports pattern matching on the fields This is not
    * necessary but is an optimization and a shorthand (i.e.
    * {@code <pos="NNPS?"> is equivalent to "<pos="NNP" | pos="NNPS">} and
    * {@code (?:<pos="NNP"> | <pos="NNPS">)}.
    * <p>
    * Here are some equivalent examples:
    * <ol>
    * <li> {@code <pos="JJ">* <pos="NNP.">+}
    * <li> {@code <pos="JJ">* <pos="NNPS?">+}
    * <li> {@code <pos="JJ">* <pos="NNP" | pos="NNPS">+}
    * <li> {@code <pos="JJ">* (?:<pos="NNP"> | <pos="NNPS">)+}
    * </ol>
    * Note that (3) and (4) are not preferred for efficiency reasons. Regex OR
    * (in example (4)) should only be used on multi-token sequences.
    * <p>
    * The Regular Expressions support named groups (<name>: ... ), unnamed
    * groups (?: ... ), and capturing groups ( ... ). The operators allowed are
    * +, ?, *, and |. The Logic Expressions (that describe each token) allow
    * grouping "( ... )", not '!', or '|', and and '&'.
    *
    * @param regex
    * @return
    */
  def compile(pattern: String) =
    openregex.Pattern.compile(pattern, (expression: String) => {
      new Function[Token, Boolean] {
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
            case caseInsensitiveDoubleQuoteStringLiteralRegex(string) =>
              val unescapedString = StringEscapeUtils.unescapeJava(string)
              new Expressions.StringCIExpression(field, unescapedString)
            case caseInsensitiveSingleQuoteStringLiteralRegex(string) =>
              new Expressions.StringCIExpression(field, string)
            case regexLiteralRegex(string) =>
              val unescapedString = string.replace("""\\""", """\""").replace("""\/""", "/")
              new Expressions.RegularExpression(field, string)
            case _ => throw new IllegalArgumentException("Value not enclosed in quote (\") or (') or (/): " + argument)
          }
        }

        val logic: LogicExpression[Token] =
          LogicExpression.compile(expression, deserializeToken andThen logicArgFromFunction[Token])

        override def apply(token: Token): Boolean = logic.apply(token)
      }
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
