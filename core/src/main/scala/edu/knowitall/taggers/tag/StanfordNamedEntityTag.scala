package edu.knowitall.taggers.tag;

import java.io.IOException
import java.util.ArrayList
import java.util.List
import org.jdom2.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.typer.StanfordNer;
import edu.knowitall.taggers.TypeHelper

/**
 * *
 * Tag Stanford named entities.
 * @author schmmd
 *
 */
case class StanfordNamedEntityTagger(name: String) extends Tagger {
  // needed for reflection
  def this(name: String, args: Seq[String]) = this(name)

  val logger = LoggerFactory.getLogger(this.getClass);
  final val DEFAULT_MODEL = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";

  override val source = "Stanford"

  val typer = StanfordNer.withDefaultModel

  def findTags(sentence: Seq[Lemmatized[ChunkedToken]]) = {
    val types = typer.apply(sentence.map(_.token))
    for (typ <- types) yield {
      TypeHelper.fromSentence(sentence, typ.name, typ.source, typ.tokenInterval)
    }
  }
}
