package edu.knowitall.taggers.tag;

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.TypeHelper
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.typer.Type

import java.util.List
import scala.collection.JavaConverters._

abstract class JavaTagger(val name: String, val source: String)
    extends Tagger[Sentence with Chunks with sentence.Lemmas] {
  override def findTags(sentence: TheSentence): Seq[Type] = {
    val lemmatizedTokens = sentence.lemmatizedTokens
    this.findTagsJava(lemmatizedTokens.asJava).asScala
  }

  def findTagsJava(sentence: java.util.List[Lemmatized[ChunkedToken]]): java.util.List[Type]

  def createType(sentence: java.util.List[Lemmatized[ChunkedToken]], interval: Interval): Type = {
    TypeHelper.fromJavaSentence(sentence, this.name, this.source, interval)
  }
}
