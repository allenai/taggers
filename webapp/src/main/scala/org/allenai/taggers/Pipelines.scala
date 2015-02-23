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
import org.allenai.common.Resource
import scala.io.Source
import org.allenai.nlpstack.core.ChunkedToken
import org.allenai.nlpstack.core.Lemmatized
import org.allenai.nlpstack.core.Tokenizer
import org.allenai.pipeline.ArtifactIo
import org.allenai.pipeline.UnknownCodeInfo

object Pipelines {
  type ChunkedSentence = Tagger.Sentence with Chunks with Lemmas
  class ChunkSentences(sentences: Producer[Iterable[String]])
    extends Producer[Iterable[ChunkedSentence]] {
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
    override def create: Iterable[ChunkedSentence] =
      sentences.get map this.apply

    def codeInfo: CodeInfo = ???

    def signature: Signature = ???
  }
  
  implicit def sentencePickler = new StringSerializable[ChunkedSentence] {
    def fromString(pickled: String): ChunkedSentence = {
      val lemmatized = pickled.split("\n").map { pickledToken =>
        val (offset, string, lemma, postag, chunk)=
          pickledToken.split("\t") match {
            case Array(offset, string, lemma, postag, chunk) =>
              (offset.toInt, string, lemma, postag, chunk)
            case _ => throw new MatchError(pickledToken)
        }
        val chunked = ChunkedToken(chunk, postag, string, offset)
        Lemmatized[ChunkedToken](chunked, lemma)
      }.toVector
      
      val plainTokens = lemmatized map (_.token)
      val originalText = Tokenizer.originalText(plainTokens)
      new Sentence(originalText) with Chunks with Lemmas with Consume {
        override val lemmatizedTokens = lemmatized
        override val tokens = plainTokens
      }
    }
    
    def toString(sentence: ChunkedSentence): String = {
      val builder = new StringBuilder(sentence.tokens.length * 25)
      for (token <- sentence.lemmatizedTokens) {
        builder.append(Iterable(
            token.token.offset,
            token.token.string,
            token.lemma,
            token.token.postag,
            token.token.chunk).mkString("\t") + "\n")
      }
      builder.toString
    }
  }
  
  implicit val chunkedSentencesArtifactIo = 
    new ArtifactIo[Iterable[ChunkedSentence], FlatArtifact] with UnknownCodeInfo {
      def read(artifact: FlatArtifact): Iterable[ChunkedSentence] = {
        Resource.using(Source.fromInputStream(artifact.read)) { source =>
          var it = source.getLines

          var sentences = Vector.empty[ChunkedSentence]
          while (it.hasNext) {
            val (tokens, rest) = it.span(_ != "")
            it = rest

            if (!tokens.isEmpty) {
              val sentence = sentencePickler.fromString(tokens.mkString("\n"))
              sentences = sentences :+ sentence
            }
            else {
              // Skip over blank lines.
              it.next()
            }
          }

          sentences
        }
      }

      def write(data: Iterable[ChunkedSentence], artifact: FlatArtifact): Unit = {
        artifact.write { writer =>
          for (sentence <- data) {
            writer.println(sentencePickler.toString(sentence))
          }
        }
      }
    }
}
