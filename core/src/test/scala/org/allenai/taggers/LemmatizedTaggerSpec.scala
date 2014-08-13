package org.allenai.taggers

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr.{Lemmatizer, Chunker, Sentence}
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.tag.LemmatizedKeywordTagger
import org.scalatest.FlatSpec
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

class LemmatizedTaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new LemmatizedKeywordTagger("JamesTagger", Seq("james"))

  "LemmatizedKeywordTagger" should "match 'james' in a sentence." in {
    val sentenceText = "Jack enjoyed a beer with James."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with Chunker with Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = defaultPostagger
      override val tokenizer = defaultTokenizer
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.size === 1)
  }
}
