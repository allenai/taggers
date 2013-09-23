package edu.knowitall.taggers

import io.Source
import java.io.File
import edu.knowitall.common.Resource.using
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.taggers.constraint.Constraint

object CompactTaggerCollection {
  def fromFile(file: File) = {
    using(Source.fromFile(file)) { input =>
    }
  }
}

case class CompactTaggerCollection(taggers: Seq[Tagger], definitions: Map[String, String]) {
  def +(tagger: Tagger) = {
    this.copy(taggers :+ tagger)
  }

  object Rule {
    val definitionSyntax = "=>"
    val taggerSyntax = ":="
    def parse(rule: String) = {
      val definitionIndex = rule.indexOf(definitionSyntax)
      val taggerIndex = rule.indexOf(taggerSyntax)

      if (definitionIndex < taggerIndex) {
        DefinitionRule(
          name = rule.substring(0, definitionIndex),
          definition = rule.substring(definitionIndex + definitionSyntax.size, rule.size))
      }
      else if (taggerIndex < definitionIndex) {
        TaggerRule.parse(
          name = rule.substring(0, taggerIndex),
          definition = rule.substring(taggerIndex + taggerSyntax.size, rule.size))
      }
      else {
        throw new MatchError("Could not break line into a rule: " + rule)
      }
    }
  }
  abstract class Rule {
    def name: String
    def definition: String
  }
  case class DefinitionRule(name: String, definition: String) {
    override def toString = s"$name ${Rule.definitionSyntax} $definition"
  }
  case class TaggerRule(name: String, tagger: String, constraints: Seq[Constraint], arguments: Seq[String]) {
    override def toString = {
      if (constraints.isEmpty && arguments.size == 1) {
        s"$name ${Rule.definitionSyntax} $definition"
      }
      else {
        val list = constraints.map(_.toString) ++ $definition
        s"$name ${Rule.taggerSyntax} {\n${list.mkString("\n")}\n}"
      }
    }
  }
}

abstract class RuleReader {
  def read(line: String): Rule
}

abstract class RuleWriter {
  def write(rule: Rule): String
}

abstract class RuleFormat extends RuleReader with RuleWriter
