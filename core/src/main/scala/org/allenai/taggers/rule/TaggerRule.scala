package org.allenai.taggers.rule

import org.allenai.taggers.constraint.Constraint
import org.allenai.taggers.tag.ConstrainedTagger
import org.allenai.taggers.tag.ConsumingTagger
import org.allenai.taggers.tag.Tagger

object TaggerRule {
  val constraintPrefix = "constraint:"
  val commentPrefix = "//"
  def parse[S <: Tagger.Sentence](name: String, tagger: String, consuming: Boolean, allArguments: Seq[String]) = {
    val (constraintStrings, arguments) = allArguments.map(_.trim).filter(!_.startsWith(commentPrefix)).partition(_.startsWith(constraintPrefix))
    val constraints = constraintStrings.map(_.drop("constraint:".length)) map (constraint => Constraint.create[S](constraint.trim))
    TaggerRule[S](name, tagger, consuming, constraints, arguments)
  }
}

/** A representation of a parsed tagger rule in the DSL. */
case class TaggerRule[S <: Tagger.Sentence](name: String, taggerIdentifier: String, consuming: Boolean, constraints: Seq[Constraint[S]], arguments: Seq[String]) extends Rule[S] {
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
    var tagger = Tagger.create[S](this.taggerIdentifier, "org.allenai.taggers.tag", name, substituted)

    // Apply constraints.
    if (!constraints.isEmpty) {
      tagger = new ConstrainedTagger(tagger, constraints)
    }

    // Make a consuming tagger.
    if (consuming) {
      tagger = new ConsumingTagger(tagger)
    }

    tagger
  }

  override def toString = {
    s"$name $definition"
  }
}
