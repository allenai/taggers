package edu.knowitall.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.constraint.VerbPhraseConstraint
import edu.knowitall.taggers.tag.ConstrainedTagger
import edu.knowitall.taggers.tag.KeywordTagger
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer

import org.scalatest.FlatSpec

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

class KeywordTaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new KeywordTagger("RoadTagger", Seq("road"))

  "a keyword tagger" should "match the last token in a sentence" in {
    val sentenceText = "The man had run down the road"
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.apply(s)

    assert(types.size === 1)
  }
}
