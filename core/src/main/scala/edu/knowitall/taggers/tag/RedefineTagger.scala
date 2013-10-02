package edu.knowitall.taggers.tag;

import java.io.IOException
import java.util.ArrayList
import java.util.List
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.taggers.TypeHelper

/**
 *
 * Rename a type to another name.
 * @author schmmd
 *
 */
case class RedefineTagger(name: String, target: String) extends Tagger {
  override val source = null

  // needed for reflection
  def this(name: String, args: Seq[String]) = this(name, args.head)

  def findTags(sentence: Seq[Lemmatized[ChunkedToken]]): Seq[Type] = {
    Seq.empty
  }

  override def findTagsWithTypes(sentence: Seq[Lemmatized[ChunkedToken]], tags: Seq[Type]): Seq[Type] = {
    // links will be lost
    tags.filter(_.name == this.target).map(typ => Type(typ.name, typ.source, typ.tokenInterval, typ.text))
  }
}
