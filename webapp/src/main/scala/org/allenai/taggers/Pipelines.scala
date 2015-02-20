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
import org.allenai.taggers.tag.Tagger
import org.allenai.pipeline.StringSerializable
import org.allenai.pipeline.FlatArtifact

object Pipelines {
  type ChunkedSentence = Tagger.Sentence with Chunks with Lemmas
  class ChunkSentences(sentences: Producer[Iterator[String]])
    extends Producer[Iterator[ChunkedSentence]] {
    val tokenizer = defaultTokenizer
    val postagger = defaultPostagger
    val chunker = new OpenNlpChunker()

    def apply(sentence: String): ChunkedSentence = this.synchronized {
      new Sentence(sentence) with Consume with Chunker with Lemmatizer {
        val tokenizer = ChunkSentences.this.tokenizer
        val postagger = ChunkSentences.this.postagger
        val chunker = ChunkSentences.this.chunker
        val lemmatizer = MorphaStemmer
      }
    }
    override def create: Iterator[ChunkedSentence] =
      sentences.get map this.apply

    def codeInfo: CodeInfo = ???

    def signature: Signature = ???
  }
  
  implicit val chunkedSentencesArtifactIo = new ArtifactIo[FlatArtifact, Iterator[ChunkedSentence]] {
    def read(artifact: FlatArtifact): Iterator[ChunkedSentence] = {
      
    }

    def write(data: Iterator[ChunkedSentence], artifact: Sentence): Unit = {
      
    }
  }
}