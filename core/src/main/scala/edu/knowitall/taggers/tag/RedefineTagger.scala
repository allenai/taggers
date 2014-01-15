package edu.knowitall.taggers.tag;

import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.taggers.TypeHelper
import edu.knowitall.repr.sentence.Sentence

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.List

/** Rename a type to another name.
  * @author schmmd
  *
  */
case class RedefineTagger(name: String, target: String) extends Tagger[Sentence] {
  override val source = null

  // needed for reflection
  def this(name: String, args: Seq[String]) = this(name, args.head)

  def findTags(sentence: TheSentence): Seq[Type] = {
    Seq.empty
  }

  override def findTagsWithTypes(sentence: TheSentence, tags: Seq[Type], consumedIndices: Seq[Int]): Seq[Type] = {
    // links will be lost
    tags.filter(_.name == this.target).map(typ => Type(typ.name, typ.source, typ.tokenInterval, typ.text))
  }
}
