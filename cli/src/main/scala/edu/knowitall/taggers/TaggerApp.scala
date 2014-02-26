package edu.knowitall.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.rule._
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

class TaggerApp(cascade: Cascade[Tagger.Sentence with Chunks with Lemmas]) {
  type Sent = Tagger.Sentence with Chunks with Lemmas
  val chunker = new OpenNlpChunker()

  def format(typ: Type) = {
    typ.name + "(" + typ.text + ")"
  }

  def process(text: String): Sent = {
    new Sentence(text) with Consume with Chunker with Lemmatizer {
      val chunker = TaggerApp.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def apply(sentence: Sent): (Seq[String], Seq[String]) = {
    val (types, extractions) = cascade apply sentence
    (types.reverse map format, extractions)
  }
}
