package edu.knowitall.taggers

import java.io.FileReader
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.util.parsing.combinator.JavaTokenParsers
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.tag.PatternTagger
import scala.util.control.Exception
import edu.knowitall.taggers.tag.Tagger

class RuleParser extends JavaTokenParsers {
  val descriptor = ident
  val taggerIdent = ident

  val valn = descriptor ~ Rule.definitionSyntax ~ ".+".r ^^ { case name ~ Rule.definitionSyntax ~ valn => DefinitionRule(name, valn) }

  val singlearg = ".+(?=\\s*\\))".r
  val singleline = "(" ~> singlearg <~ ")"
  val multiarg = "[^}].*".r
  val multiline = "{" ~> rep(multiarg) <~ "}" ^^ { seq => seq.map(_.trim) }
  val args = singleline ^^ { arg => Seq(arg.trim) } | multiline
  val tagger = descriptor ~ Rule.taggerSyntax ~ taggerIdent ~ args ^^ {
    case descriptor ~ Rule.taggerSyntax ~ taggerIdent ~ args =>
      TaggerRule.parse(descriptor, taggerIdent, args)
  }

  val rule: Parser[Rule] = valn | tagger
  val collection = rep(rule)
}

object ParseRule extends RuleParser {
  def parse(string: String) = parseAll(collection, string)
  def main(args: Array[String]) = {
    val reader = new FileReader(args(0))
    val rules = parseAll(collection, reader) match {
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

abstract class Rule {
  def name: String
  def definition: String
}

case class DefinitionRule(name: String, definition: String) extends Rule {
  override def toString = s"$name ${Rule.definitionSyntax} $definition"

  def replace(string: String) = {
    string.replaceAll("\\$\\{" + name + "}", definition)
  }
}

object TaggerRule {
  def parse(name: String, tagger: String, allArguments: Seq[String]) = {
    val constraintPrefix = "constraint:"
    val (constraintStrings, arguments) = allArguments.partition(_.startsWith(constraintPrefix))
    val constraints = constraintStrings.map(_.drop("constraint:".length)) map (constraint => Constraint.create(constraint.trim))
    TaggerRule(name, tagger, constraints, arguments)
  }
}

case class TaggerRule(name: String, taggerIdentifier: String, constraints: Seq[Constraint], arguments: Seq[String]) extends Rule {
  def definition = {
    if (constraints.isEmpty && arguments.size == 1) {
      s"${Rule.taggerSyntax} $taggerIdentifier( ${arguments.head} )"
    } else {
      val list = constraints.map(_.toString) ++ arguments
      s"${Rule.taggerSyntax} $taggerIdentifier {\n${list.map(" " * 4 + _).mkString("\n")}\n}"
    }
  }

  def instantiate(definitions: Iterable[DefinitionRule]) = {
    val substituted = arguments.map(arg => definitions.foldLeft(arg) { case (arg, defn) => defn.replace(arg) })
    val tagger = Tagger.create(this.taggerIdentifier, "edu.knowitall.taggers.tag", Array[Object](name, substituted.asJava))

    // apply constraints
    constraints foreach tagger.constrain

    tagger
  }

  override def toString = {
    s"$name $definition"
  }
}
