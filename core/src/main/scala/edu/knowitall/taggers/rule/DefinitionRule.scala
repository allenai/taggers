package edu.knowitall.taggers.rule

import edu.knowitall.repr.sentence.Sentence

/** A representation of a parsed definition rule in the DSL. */
case class DefinitionRule[-S <: Sentence](name: String, definition: String) extends Rule[S] {
  override def toString = s"$name ${Rule.definitionSyntax} $definition"

  def replace(string: String) = {
    string.replaceAll("\\$\\{" + name + "}", definition)
  }
}

