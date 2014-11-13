package org.allenai.taggers

import spray.json._

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr.Chunker
import org.allenai.nlpstack.core.repr.Chunks
import org.allenai.nlpstack.core.repr.Lemmas
import org.allenai.nlpstack.core.repr.Lemmatizer
import org.allenai.nlpstack.core.repr.Sentence
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.tag.Tagger
import org.allenai.nlpstack.core.Lemmatized
import org.allenai.nlpstack.core.ChunkedToken

class Processor {
  type Sent = Tagger.Sentence with Chunks with Lemmas

  // External NLP tools that are used to build the expected type from a sentence string.
  lazy val tokenizer = defaultTokenizer
  lazy val postagger = defaultPostagger
  lazy val chunker = new OpenNlpChunker()

  /** Build the NLP representation of a sentence string. */
  def apply(text: String): Sent = {
    new Sentence(text) with Consume with Chunker with Lemmatizer {
      val tokenizer = Processor.this.tokenizer
      val postagger = Processor.this.postagger
      val chunker = Processor.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }   
}

object Processor {
  type Sent = Tagger.Sentence with Chunks with Lemmas
  
  object Formats extends DefaultJsonProtocol {
    val tokenJsonFormat = new RootJsonFormat[Lemmatized[ChunkedToken]] {
      override def read(json: JsValue): Lemmatized[ChunkedToken] = {
        json.asJsObject.getFields("lemma", "token") match {
          case Seq(JsString(lemma), token) => 
            new Lemmatized[ChunkedToken](token.convertTo[ChunkedToken], lemma)
          case _ => throw new IllegalArgumentException("Expected JsObject with lemma and token: " +
              json.compactPrint)
        }
      }
      
      override def write(token: Lemmatized[ChunkedToken]): JsValue = {
        JsObject("lemma" -> JsString(token.lemma),
            "token" -> token.token.toJson)
      }
    }

    val sentenceJsonFormat = new RootJsonFormat[Sent] {
      override def read(json: JsValue): Sent = {
        json.asJsObject.getFields("text", "tokens") match {
          case Seq(JsString(text), JsArray(tokenList)) =>
            new Sentence(text) with Consume with Chunks with Lemmas {
              override def lemmatizedTokens = tokenList map tokenJsonFormat.read
              override def tokens = lemmatizedTokens map (_.token)
            }
          case _ => throw new IllegalArgumentException("Expected JsObject with text and tokens: " +
              json.compactPrint)
        }
      }
      
      override def write(sent: Sent): JsValue = JsObject(
          "text" -> JsString(sent.text),
          "tokens" -> JsArray((sent.lemmatizedTokens map tokenJsonFormat.write).toList))
    }
  }
}