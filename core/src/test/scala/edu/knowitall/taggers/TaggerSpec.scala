package edu.knowitall.taggers

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import org.scalatest.FlatSpec
import edu.knowitall.taggers.constraint.VerbPhraseConstraint
import edu.knowitall.taggers.tag.KeywordTagger
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence
import edu.knowitall.taggers.tag.ConstrainedTagger

class TaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new ConstrainedTagger(new KeywordTagger("Run", Seq("run")), Seq(VerbPhraseConstraint))

  "runTagger" should "match verb run" in {
    val sentenceText = "The man had run down the road."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.tags(s)

    assert(types.head.name === "Run")
    assert(types.head.text === "run")
  }

  "runTagger" should "not match noun run" in {
    val sentenceText = "The man went for a run."
    val opennlpChunker = new OpenNlpChunker
    val s = new Sentence(sentenceText) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    val types = runTagger.tags(s)

    assert(types.isEmpty)
  }
}
