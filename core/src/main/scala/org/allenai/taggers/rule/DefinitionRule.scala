package org.allenai.taggers.rule

import edu.knowitall.repr.sentence.Sentence
import org.allenai.taggers.tag.Tagger

/** A representation of a parsed definition rule in the DSL. */
case class DefinitionRule[-S <: Tagger.Sentence](name: String, definition: String) extends Rule[S] {
  override def toString = s"$name ${Rule.definitionSyntax} $definition"

  def replace(string: String) = {
    string.replaceAll("\\$\\{" + name + "}", definition)
  }
}

