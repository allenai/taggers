package edu.knowitall.taggers

import scala.collection.JavaConverters._

import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.typer.Type
import edu.knowitall.collection.immutable.Interval

object TypeHelper {
  def fromSentence(sentence: Seq[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val tokens = sentence.slice(interval.start, interval.end).iterator
    this.fromSentenceIterator(tokens, name, source, interval)
  }

  def fromJavaSentence(sentence: java.util.List[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val tokens = sentence.subList(interval.start, interval.end).iterator.asScala
    this.fromSentenceIterator(tokens, name, source, interval)
  }

  private def fromSentenceIterator(tokens: Iterator[Lemmatized[ChunkedToken]], name: String, source: String, interval: Interval): Type = {
    val text = tokens.map(_.string).mkString(" ").trim
    new Type(name, source, interval, text);
  }
}