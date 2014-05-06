package org.allenai.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import org.allenai.taggers.rule._
import org.allenai.taggers.tag.Tagger
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

/** A self-contained class for processing sentences and running taggers
  * over chunked sentences. */
class ChunkedTaggerApp(cascade: Cascade[Tagger.Sentence with Chunks with Lemmas]) {
  type Sent = Tagger.Sentence with Chunks with Lemmas
  val chunker = new OpenNlpChunker()

  def format(typ: Type) = {
    typ.name + "(" + typ.text + ")"
  }

  def process(text: String): Sent = this.synchronized {
    new Sentence(text) with Consume with Chunker with Lemmatizer {
      val chunker = ChunkedTaggerApp.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def apply(sentence: Sent): (Seq[String], Seq[String]) = {
    val (types, extractions) = cascade.apply(sentence)
    (types.reverse map format, extractions)
  }
}
