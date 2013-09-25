package edu.knowitall.taggers

import scala.collection.JavaConverters._

import org.scalatest.FlatSpec

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.taggers.tag.TaggerCollection
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.stem.MorphaStemmer

class PatternTaggerSpec extends FlatSpec{

  "WorldCandy PatternTagger" should "match an occurrence from a sequence of Type Nationality and Type Candy" in{
    //load keyword taggers candies.xml, and nationalities.xml from resources
    //load PatternTagger worldcandies.xml from resources
    val taggerCollection = TaggerCollection.fromPath(getClass.getResource("/edu/knowitall/taggers").getPath())

    //test sentence that should be tagged as
    // WorldCandy{(3,5)}
    val testSentence = "Vernon enjoys eating Argentinian Licorice."

    val chunker = new OpenNlpChunker();

    val tokens = chunker.chunk(testSentence) map MorphaStemmer.lemmatizeToken

    //Tag the sentence with the loaded taggers
    val types = taggerCollection.tag(tokens.asJava).asScala

    //matching interval should be [3,5)
    val worldCandyInterval = Interval.open(3,5)

    //iterate over all the types returned
    //searching for a type that matches
    //the worldCandyInterval
    val targetTypeOption = types.find(_.interval == worldCandyInterval)

    //assert that the worldCandyInterval has been matched
    //and that it has been tagged as a ''World Candy''
    assert(targetTypeOption.isDefined)
    assert(targetTypeOption.get.descriptor() == "WorldCandy")
  }
}
