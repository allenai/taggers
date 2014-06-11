package org.allenai.taggers.tag;

import org.allenai.common.immutable.Interval
import org.allenai.repr.sentence
import org.allenai.repr.sentence.Chunks
import org.allenai.repr.sentence.Sentence
import org.allenai.taggers.TypeHelper
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.nlpstack.typer.Type

import java.util.List
import scala.collection.JavaConverters._

abstract class JavaTagger(val name: String, val source: String)
    extends Tagger[Tagger.Sentence with Chunks with sentence.Lemmas] {
  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    val lemmatizedTokens = sentence.lemmatizedTokens

    // change consumed token attributes into empty string
    val consumedTokens = lemmatizedTokens.zipWithIndex.map { case (token, i) =>
      if (sentence.consumingTypes(i).isDefined) {
        new Lemmatized[ChunkedToken](ChunkedToken("", "", "", token.token.offset), "")
      }
      else {
        token
      }
    }

    this.findTagsJava(lemmatizedTokens.asJava).asScala
  }

  def findTagsJava(sentence: java.util.List[Lemmatized[ChunkedToken]]): java.util.List[Type]

  def createType(sentence: java.util.List[Lemmatized[ChunkedToken]], interval: Interval): Type = {
    TypeHelper.fromJavaSentence(sentence, this.name, this.source, interval)
  }
}
