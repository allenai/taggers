package org.allenai.taggers

import org.allenai.common.immutable.Interval
import org.allenai.repr.sentence.Sentence
import org.allenai.repr.sentence.Tokens
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.nlpstack.tokenize.Token
import org.allenai.nlpstack.tokenize.Tokenizer
import org.allenai.nlpstack.typer.Type

import scala.collection.JavaConverters._

object TypeHelper {
  def fromSentence(sentence: Sentence with Tokens, name: String, source: String, interval: Interval): Type = {
    val tokens = sentence.tokens.slice(interval.start, interval.end)
    this.fromSentenceIterator(tokens, name, source, interval)
  }

  def fromJavaSentence(sentence: java.util.List[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val tokens = sentence.subList(interval.start, interval.end).asScala
    this.fromSentenceIterator(tokens.map(_.token), name, source, interval)
  }

  private def fromSentenceIterator(tokens: Seq[Token], name: String, source: String, interval: Interval): Type = {
    val text = Tokenizer.originalText(tokens, tokens.head.offsets.start)
    Type(name, source, interval, text)
  }
}
