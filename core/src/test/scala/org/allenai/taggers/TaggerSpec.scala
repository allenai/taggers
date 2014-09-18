package org.allenai.taggers

import org.allenai.nlpstack.core.repr
import org.allenai.nlpstack.core.repr.Sentence
import org.allenai.taggers.constraint.VerbPhraseConstraint
import org.allenai.taggers.tag.ConstrainedTagger
import org.allenai.taggers.tag.KeywordTagger
import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.scalatest.FlatSpec
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import org.allenai.nlpstack.postag.OpenNlpPostagger
import org.allenai.nlpstack.tokenize.SimpleEnglishTokenizer

class TaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new ConstrainedTagger(new KeywordTagger("Run", Seq("run")), Seq(VerbPhraseConstraint))

  "runTagger" should "match verb run" in {
    val sentenceText = "The man had run down the road."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with repr.Chunker with repr.Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = new OpenNlpPostagger
      override val tokenizer = new SimpleEnglishTokenizer
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.head.name === "Run")
    assert(types.head.text === "run")
  }

  "runTagger" should "not match noun run" in {
    val sentenceText = "The man went for a run."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with repr.Chunker with repr.Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = new OpenNlpPostagger
      override val tokenizer = new SimpleEnglishTokenizer
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.isEmpty)
  }
}
