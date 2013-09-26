package edu.knowitall.taggers

import scala.collection.JavaConverters._

import org.scalatest.FlatSpec

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.taggers.tag.TaggerCollection
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.stem.MorphaStemmer

class PatternTaggerSpec extends FlatSpec {
  val chunker = new OpenNlpChunker();

  "WorldCandy PatternTagger" should "match an occurrence from a sequence of Type Nationality and Type Candy" in {
    val taggers = """
        Candy := CaseInsensitiveKeywordTagger {
          choclate
          gum
          taffy
          licorice
        }
        Nationality := CaseInsensitiveKeywordTagger {
          argentinian
        }
        WorldCandy := PatternTagger {
          <type='Nationality'>+ <type='Candy'>+
        }
      """
    val taggerCollection = CompactTaggerCollection.fromString(taggers).toTaggerCollection

    //test sentence that should be tagged as
    // WorldCandy{(3,5)}
    val testSentence = "Vernon enjoys eating Argentinian Licorice."

    val tokens = chunker.chunk(testSentence) map MorphaStemmer.lemmatizeToken

    //Tag the sentence with the loaded taggers
    val types = taggerCollection.tag(tokens.asJava).asScala

    //matching interval should be [3,5)
    val worldCandyInterval = Interval.open(3, 5)

    //iterate over all the types returned
    //searching for a type that matches
    //the worldCandyInterval
    val targetTypeOption = types.find(_.interval == worldCandyInterval)

    //assert that the worldCandyInterval has been matched
    //and that it has been tagged as a ''World Candy''
    assert(targetTypeOption.isDefined)
    assert(targetTypeOption.get.descriptor() === "WorldCandy")
  }

  "type fields in PatternTagger" should "match correctly" in {
    val taggers =
      """AnimalTagger := KeywordTagger{
           the large cat
         }
         TypeTaggerTest := PatternTagger {
           <type = 'AnimalTagger' >
         }
         TypeStartTaggerTest := PatternTagger {
           <typeStart = 'AnimalTagger' >
         }
         TypeContTaggerTest := PatternTagger {
           <typeCont = 'AnimalTagger' >
         }
         TypeEndTaggerTest := PatternTagger {
           (<aName>:<typeEnd = 'AnimalTagger' >)
         }
      """

    val taggerCollection = CompactTaggerCollection.fromString(taggers).toTaggerCollection

    val testSentence = "I once saw the large cat on a couch ."

    val tokens = chunker.chunk(testSentence) map MorphaStemmer.lemmatizeToken

    val types = taggerCollection.tag(tokens.asJava).asScala

    val typeTypes = types.filter(_.descriptor == "TypeTaggerTest")
    assert(typeTypes.size === 3)
    assert(typeTypes.map(_.text).toSet == Set("the", "large", "cat"))

    val typeStartTypes = types.filter(_.descriptor == "TypeStartTaggerTest")
    assert(typeStartTypes.size === 1)
    assert(typeStartTypes.headOption.map(_.text).get == "the")

    val typeEndTypes = types.filter(_.descriptor == "TypeEndTaggerTest")
    assert(typeEndTypes.size === 1)
    assert(typeEndTypes.headOption.map(_.text).get == "cat")
    
    val typeEndANameTypes = types.filter(_.descriptor == "TypeEndTaggerTest.aName")
    assert((typeEndANameTypes.size == 1))
    assert(typeEndANameTypes.headOption.map(_.text).get == "cat")

    val typeContTypes = types.filter(_.descriptor == "TypeContTaggerTest")
    assert(typeContTypes.size === 1)
    assert(typeContTypes.headOption.map(_.text).get == "large")
  }
}
