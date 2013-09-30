package edu.knowitall.taggers

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

import org.scalatest.FlatSpec

import edu.knowitall.taggers.constraint.VerbPhraseConstraint
import edu.knowitall.taggers.tag.KeywordTagger
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer

class TaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();
  val morpha = new MorphaStemmer();

  val runTagger = new KeywordTagger("Run", Seq("run"))
  runTagger.constrain(new VerbPhraseConstraint())

  "runTagger" should "match verb run" in {
    val sentence = "The man had run down the road."

    val tokens = chunker.chunk(sentence) map MorphaStemmer.lemmatizeToken

    val types = runTagger.tags(tokens)

    assert(types.head.name === "Run")
    assert(types.head.text === "run")
  }

  "runTagger" should "not match noun run" in {
    val sentence = "The man went for a run."

    val tokens = chunker.chunk(sentence) map MorphaStemmer.lemmatizeToken

    val types = runTagger.tags(tokens)

    assert(types.isEmpty)
  }
}
