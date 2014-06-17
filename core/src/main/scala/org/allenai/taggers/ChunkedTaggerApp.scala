package org.allenai.taggers

import org.allenai.repr.sentence
import org.allenai.repr.sentence.Chunker
import org.allenai.repr.sentence.Chunks
import org.allenai.repr.sentence.Lemmas
import org.allenai.repr.sentence.Lemmatizer
import org.allenai.repr.sentence.Sentence
import org.allenai.taggers.rule._
import org.allenai.taggers.tag.Tagger
import org.allenai.nlpstack.tokenize.SimpleEnglishTokenizer
import org.allenai.nlpstack.postag.OpenNlpPostagger
import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.typer.Type

/** A self-contained class for processing sentences and running taggers
  * over chunked sentences. */
class ChunkedTaggerApp(cascade: Cascade[Tagger.Sentence with Chunks with Lemmas]) {
  type Sent = Tagger.Sentence with Chunks with Lemmas
  val tokenizer = new SimpleEnglishTokenizer()
  val postagger = new OpenNlpPostagger()
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
