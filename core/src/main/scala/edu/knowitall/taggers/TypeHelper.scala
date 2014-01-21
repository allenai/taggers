package edu.knowitall.taggers

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Tokenized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.tokenize.Token
import edu.knowitall.tool.tokenize.Tokenizer
import edu.knowitall.tool.typer.Type

import scala.collection.JavaConverters._

object TypeHelper {
  def fromSentence(sentence: Sentence with Tokenized, name: String, source: String, interval: Interval): Type = {
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
