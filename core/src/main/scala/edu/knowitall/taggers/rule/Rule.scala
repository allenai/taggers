package edu.knowitall.taggers.rule

import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.tag.ConstrainedTagger
import edu.knowitall.taggers.tag.PatternTagger
import edu.knowitall.taggers.tag.Tagger

import java.io.FileReader
import java.io.Reader
import java.io.StringReader
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.util.control.Exception
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.{ Try, Success }

/** A parser combinator for parsing rules.
  *
  * There are two types of rules, definitions and tagger rules.
  *  1.  Definitions are used as a text subsitution on subsequent lines.
  *  2.  Tagger rules provide the necessary information to instantiate a tagger.
  *
  * The DSL also allows coments with //.
  *
  * For examples, see test cases.
  */
class RuleParserCombinator[S <: Sentence] extends JavaTokenParsers {
  val name = ident
  val taggerIdent = ident

  /** A regular expression to match a line that starts with spaces,
    * then "//" followed by anything. */
  val comment = "(?m)^\\s*//.*$".r

  // A regex that matches a line containing only spaces.
  val blankLine = "(?m)^\\s*$".r

  // Lines that can be thrown out or safetly ignored.
  val ignore = comment | blankLine

  // An extractor for a redefinition.
  val valn = name ~ Rule.definitionSyntax ~! ".+".r ^^ { case name ~ Rule.definitionSyntax ~ valn => DefinitionRule[S](name, valn) }

  // An extractor for an argument to a tagger.
  val singlearg = ".+(?=\\s*\\))".r

  // Single line arguments use parentheses
  val singleline = "(" ~> singlearg <~ ")"

  // Multi-line arguments use braces
  val multiarg = "[^}].*".r
  val multiline = "{" ~> rep(multiarg) <~ "}" ^^ { seq => seq.map(_.trim) }

  // An extractor for a tagger.
  val args = singleline ^^ { arg => Seq(arg.trim) } | multiline
  val tagger = name ~ Rule.taggerSyntax ~! taggerIdent ~! args ^^ {
    case name ~ Rule.taggerSyntax ~ taggerIdent ~ args =>
      TaggerRule.parse[S](name, taggerIdent, args)
  }

  // A rule is either a tagger or a definition.
  val rule: Parser[Rule[S]] = (ignore*) ~> (valn | tagger) <~ (ignore*)

  // An extractor for a collection of rules.
  val collection = rep(rule)
}

/** A helper class for parsing rules using RuleParserCombinator. */
class RuleParser[S <: Sentence] extends RuleParserCombinator[S] {
  def parse(string: String): Try[List[Rule[S]]] = this.parse(new StringReader(string))
  def parse(reader: Reader): Try[List[Rule[S]]] = parseAll(collection, reader) match {
    case this.Success(ast, _) => scala.util.Success(ast)
    case this.NoSuccess(err, next) => Try(throw new IllegalArgumentException("failed to parse rule " +
      "(line " + next.pos.line + ", column " + next.pos.column + "):\n" +
      err + "\n" +
      next.pos.longString))
  }
}

object Rule {
  val definitionSyntax = "=>"
  val taggerSyntax = ":="
}

abstract class Rule[-S <: Sentence] {
  def name: String
  def definition: String
}
