package org.allenai.taggers

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr.{Chunker, Lemmatizer, Sentence}
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.tag.KeywordTagger

import org.scalatest.FlatSpec

class KeywordTaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  def parse(s: String) = {
    new Sentence(s) with Chunker with Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = defaultPostagger
      override val tokenizer = defaultTokenizer
      override val lemmatizer = MorphaStemmer
    }
  }

  "a keyword tagger" should "match the last token in a sentence" in {
    val runTagger = new KeywordTagger("RoadTagger", Seq("road"))
    val sentenceText = "The man had run down the road"
    val opennlpChunker = new OpenNlpChunker

    val types = runTagger.apply(parse(sentenceText))

    assert(types.size === 1)
  }

  "a multi-word keyword tagger" should "match correctly" in {
    val tagger = new KeywordTagger("RoadTagger", Seq("rail road"))
    val sentenceText = "The man had run down the rail road"
    val opennlpChunker = new OpenNlpChunker

    val types = tagger.apply(parse(sentenceText))

    assert(types.size === 1)
  }

  "a multi-word keyword tagger" should "match twice" in {
    val tagger = new KeywordTagger("RoadTagger", Seq("rail road"))
    val sentenceText = "rail road oh my rail road"
    val opennlpChunker = new OpenNlpChunker

    val types = tagger.apply(parse(sentenceText))

    assert(types.size === 2)
  }
}
