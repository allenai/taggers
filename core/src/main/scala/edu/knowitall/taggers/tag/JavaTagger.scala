package edu.knowitall.taggers.tag;

import java.util.List
import scala.collection.JavaConverters._
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.taggers.TypeHelper
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.typer.Type
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence

abstract class JavaTagger(val name: String, val source: String)
    extends Tagger[Sentence with Chunked with sentence.Lemmatized] {
  override def findTags(sentence: TheSentence): Seq[Type] = {
    val lemmatizedTokens = sentence.lemmatizedTokens
    this.findTagsJava(lemmatizedTokens.asJava).asScala
  }

  def findTagsJava(sentence: java.util.List[Lemmatized[ChunkedToken]]): java.util.List[Type]

  def createType(sentence: java.util.List[Lemmatized[ChunkedToken]], interval: Interval): Type = {
    TypeHelper.fromJavaSentence(sentence, this.name, this.source, interval)
  }
}
