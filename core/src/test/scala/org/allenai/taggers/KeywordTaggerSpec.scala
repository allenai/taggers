package org.allenai.taggers

import org.allenai.nlpstack.core.repr
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

class KeywordTaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new KeywordTagger("RoadTagger", Seq("road"))

  "a keyword tagger" should "match the last token in a sentence" in {
    val sentenceText = "The man had run down the road"
    val opennlpChunker = new OpenNlpChunker
    val s = new repr.Sentence(sentenceText) with repr.Chunker with repr.Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val postagger = new OpenNlpPostagger
      override val tokenizer = new SimpleEnglishTokenizer
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.size === 1)
  }
}
