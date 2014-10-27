package org.allenai.taggers

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr.{Chunker, Lemmatizer, Sentence}
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.tag.NpChunkKeywordTagger

import org.scalatest.FlatSpec

class NpChunkKeywordTaggerSpec extends FlatSpec {
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

  "a np keyword tagger" should "match the chunk containing the keyword" in {
    val runTagger = new NpChunkKeywordTagger("person", Seq("man", "woman"))
    val sentenceText = "The old man had run down the road."
    val opennlpChunker = new OpenNlpChunker

    val types = runTagger.apply(parse(sentenceText))

    assert(types.size === 1)
    assert(types.head.text === "The old man")
  }

  "a np keyword tagger matching outside an np chunk" should "have no matches" in {
    val runTagger = new NpChunkKeywordTagger("prep", Seq("up", "down"))
    val sentenceText = "The old man had run down the road."
    val opennlpChunker = new OpenNlpChunker

    val sentence = parse(sentenceText)
    val types = runTagger.apply(sentence)

    assert(types.size === 0)
  }
}
