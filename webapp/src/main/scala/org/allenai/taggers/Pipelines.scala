package org.allenai.taggers

import org.allenai.pipeline.Producer
import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.core.repr.Chunks
import org.allenai.nlpstack.core.repr.Lemmas
import org.allenai.nlpstack.core.repr.Lemmatizer
import org.allenai.nlpstack.core.repr.Sentence
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.core.repr.Chunker
import org.allenai.pipeline.CodeInfo
import org.allenai.pipeline.Signature

class ChunkSentences(sentences: Producer[Iterator[String]], confidenceThreshold: Double)
extends Producer[Iterator[Tagger.Sentence with Chunks with Lemmas]] {
  type Sent = Tagger.Sentence with Chunks with Lemmas
  val tokenizer = defaultTokenizer
  val postagger = defaultPostagger
  val chunker = new OpenNlpChunker()

  def apply(sentence: String): Sent = this.synchronized {
    new Sentence(sentence) with Consume with Chunker with Lemmatizer {
      val tokenizer = ChunkSentences.this.tokenizer
      val postagger = ChunkSentences.this.postagger
      val chunker = ChunkSentences.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }
  override def create: Iterator[Tagger.Sentence with Chunks with Lemmas] = 
    sentences.get map this.apply

  def codeInfo: CodeInfo = ???

  def signature: Signature = ???
}