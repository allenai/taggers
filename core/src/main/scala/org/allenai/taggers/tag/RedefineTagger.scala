package org.allenai.taggers.tag;

import org.allenai.nlpstack.core.typer.Type
import org.allenai.nlpstack.core.ChunkedToken
import org.allenai.nlpstack.core.Lemmatized
import org.allenai.taggers.TypeHelper
import org.allenai.nlpstack.core.repr.Sentence

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.List

/** Rename a type to another name.
  * @author schmmd
  *
  */
case class RedefineTagger(name: String, target: String) extends Tagger[Tagger.Sentence] {
  override val source = null

  // needed for reflection
  def this(name: String, args: Seq[String]) = this(name, args.head)

  override def typecheck(definedTypes: Set[String]) = definedTypes contains target

  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    // Links will be lost on redefined types.
    // If any part of a type is consumed, it will not be redefined.
    for {
      tag <- types

      // Only use the specified name.
      if tag.name == this.target

      // Don't look at any types that are partially or wholly consumed.
      if tag.tokenInterval forall (i => !(sentence.consumedTypes(i, types) contains tag))
    } yield (Type(name, tag.source, tag.tokenInterval, tag.text))
  }
}
