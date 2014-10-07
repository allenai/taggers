package org.allenai.taggers

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr._
import org.allenai.nlpstack.core.typer.Type
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.tag.Tagger

/** A self-contained class for processing sentences and running taggers
  * over chunked sentences. */
class ChunkedTaggerApp(
  cascade: Cascade[Tagger.Sentence with Chunks with Lemmas],
  postagger: org.allenai.nlpstack.core.Postagger = defaultPostagger
) {
  type Sent = Tagger.Sentence with Chunks with Lemmas
  val tokenizer = defaultTokenizer
  val chunker = new OpenNlpChunker()

  def format(typ: Type) = {
    typ.name + "(" + typ.text + ")"
  }

  def process(text: String): Sent = this.synchronized {
    new Sentence(text) with Consume with Chunker with Lemmatizer {
      val tokenizer = ChunkedTaggerApp.this.tokenizer
      val postagger = ChunkedTaggerApp.this.postagger
      val chunker = ChunkedTaggerApp.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def apply(sentence: Sent): (Seq[String], Seq[String]) = {
    val (types, extractions) = cascade.apply(sentence)
    (types.reverse map format, extractions)
  }
}
