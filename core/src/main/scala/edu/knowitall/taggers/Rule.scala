package edu.knowitall.taggers

import java.io.FileReader
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.util.parsing.combinator.JavaTokenParsers
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.tag.PatternTagger
import scala.util.control.Exception
import edu.knowitall.taggers.tag.Tagger
import java.io.Reader
import edu.knowitall.repr.sentence.Sentence

class RuleParser[S <: Sentence] extends JavaTokenParsers {
  val name = ident
  val taggerIdent = ident

  val comment = "(?:(?:\\s*//.*\\n)|(?:\\s*\\n))*".r

  val valn = comment ~> name ~ Rule.definitionSyntax ~ ".+".r ^^ { case name ~ Rule.definitionSyntax ~ valn => DefinitionRule[S](name, valn) }

  val singlearg = ".+(?=\\s*\\))".r
  val singleline = "(" ~> singlearg <~ ")"
  val multiarg = "[^}].*".r
  val multiline = "{" ~> rep(multiarg) <~ "}" ^^ { seq => seq.map(_.trim) }
  val args = singleline ^^ { arg => Seq(arg.trim) } | multiline
  val tagger = comment ~> name ~ Rule.taggerSyntax ~ taggerIdent ~ args ^^ {
    case name ~ Rule.taggerSyntax ~ taggerIdent ~ args =>
      TaggerRule.parse[S](name, taggerIdent, args)
  }

  val rule: Parser[Rule[S]] = valn | tagger
  val collection = rep(rule)
}

class ParseRule[S <: Sentence] extends RuleParser[S] {
  def parse(string: String) = parseAll(collection, string)
  def parse(reader: Reader) = parseAll(collection, reader)
  def main(args: Array[String]) = {
    val reader = new FileReader(args(0))
    val rules = this.parse(reader) match {
      case Success(rules, _) => rules
      case fail: Failure =>
        throw new IllegalArgumentException("improper syntax. " + fail.msg)
      case error: Error =>
        throw new IllegalArgumentException("error")
    }

    rules foreach println
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

case class DefinitionRule[-S <: Sentence](name: String, definition: String) extends Rule[S] {
  override def toString = s"$name ${Rule.definitionSyntax} $definition"

  def replace(string: String) = {
    string.replaceAll("\\$\\{" + name + "}", definition)
  }
}

object TaggerRule {
  val constraintPrefix = "constraint:"
  val commentPrefix = "//"
  def parse[S <: Sentence](name: String, tagger: String, allArguments: Seq[String]) = {
    val (constraintStrings, arguments) = allArguments.map(_.trim).filter(!_.startsWith(commentPrefix)).partition(_.startsWith(constraintPrefix))
    val constraints = constraintStrings.map(_.drop("constraint:".length)) map (constraint => Constraint.create[S](constraint.trim))
    TaggerRule[S](name, tagger, constraints, arguments)
  }
}

case class TaggerRule[S <: Sentence](name: String, taggerIdentifier: String, constraints: Seq[Constraint[S]], arguments: Seq[String]) extends Rule[S] {
  def definition = {
    if (constraints.isEmpty && arguments.size == 1) {
      s"${Rule.taggerSyntax} $taggerIdentifier( ${arguments.head} )"
    } else {
      val list = constraints.map(_.toString) ++ arguments
      s"${Rule.taggerSyntax} $taggerIdentifier {\n${list.map(" " * 4 + _).mkString("\n")}\n}"
    }
  }

  def instantiate(definitions: Iterable[DefinitionRule[S]]): Tagger[S] = {
    val substituted = arguments.map(arg => definitions.foldLeft(arg) { case (arg, defn) => defn.replace(arg) })
    val tagger = Tagger.create[S](this.taggerIdentifier, "edu.knowitall.taggers.tag", name, substituted)

    // apply constraints
    // constraints foreach tagger.constrain

    tagger
  }

  override def toString = {
    s"$name $definition"
  }
}
