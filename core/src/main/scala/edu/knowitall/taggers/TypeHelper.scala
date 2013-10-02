package edu.knowitall.taggers

import scala.collection.JavaConverters._

import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.typer.Type
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.tokenize.Tokenizer

object TypeHelper {
  def fromSentence(sentence: Seq[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val tokens = sentence.slice(interval.start, interval.end)
    this.fromSentenceIterator(tokens, name, source, interval)
  }

  def fromJavaSentence(sentence: java.util.List[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val tokens = sentence.subList(interval.start, interval.end).asScala
    this.fromSentenceIterator(tokens, name, source, interval)
  }

  private def fromSentenceIterator(tokens: Seq[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val rawTokens = tokens.map(_.token)
    val text = Tokenizer.originalText(rawTokens, rawTokens.head.offsets.start)
    Type(name, source, interval, text)
  }
}
