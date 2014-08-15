package org.allenai.taggers

import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr.{Chunker, Lemmatizer, Sentence}
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.constraint.VerbPhraseConstraint
import org.allenai.taggers.tag.{ConstrainedTagger, KeywordTagger}

import org.scalatest.FlatSpec

class TaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new ConstrainedTagger(new KeywordTagger("Run", Seq("run")), Seq(VerbPhraseConstraint))

  "runTagger" should "match verb run" in {
    val sentenceText = "The man had run down the road."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with Chunker with Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = defaultPostagger
      override val tokenizer = defaultTokenizer
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.head.name === "Run")
    assert(types.head.text === "run")
  }

  "runTagger" should "not match noun run" in {
    val sentenceText = "The man went for a run."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with Chunker with Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = defaultPostagger
      override val tokenizer = defaultTokenizer
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.isEmpty)
  }
}
