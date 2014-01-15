package edu.knowitall.taggers.rule

import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.tag.ConstrainedTagger
import edu.knowitall.taggers.tag.Tagger

object TaggerRule {
  val constraintPrefix = "constraint:"
  val commentPrefix = "//"
  def parse[S <: Sentence](name: String, tagger: String, allArguments: Seq[String]) = {
    val (constraintStrings, arguments) = allArguments.map(_.trim).filter(!_.startsWith(commentPrefix)).partition(_.startsWith(constraintPrefix))
    val constraints = constraintStrings.map(_.drop("constraint:".length)) map (constraint => Constraint.create[S](constraint.trim))
    TaggerRule[S](name, tagger, constraints, arguments)
  }
}

/** A representation of a parsed tagger rule in the DSL. */
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
    constraints match {
      case Seq() => tagger
      case constraints => new ConstrainedTagger(tagger, constraints)
    }
  }

  override def toString = {
    s"$name $definition"
  }
}
