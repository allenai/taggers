package edu.knowitall.taggers

import scala.collection.JavaConverters._

import org.scalatest.FlatSpec

import edu.knowitall.collection.immutable.Interval
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

        //coments
      
        //more comments
      
      
        Nationality := CaseInsensitiveKeywordTagger {
          argentinian
          //do inTagger comments also still work?
        }
      
      //more crazy := {comments} <asdf>+
        WorldCandy := PatternTagger {
          <type='Nationality'>+ <type='Candy'>+
        }
      """
    val taggerCollection = TaggerCollection.fromString(taggers)

    //test sentence that should be tagged as
    // WorldCandy{(3,5)}
    val testSentence = "Vernon enjoys eating Argentinian Licorice."

    val tokens = chunker.chunk(testSentence) map MorphaStemmer.lemmatizeToken

    //Tag the sentence with the loaded taggers
    val types = taggerCollection.tag(tokens)

    //matching interval should be [3,5)
    val worldCandyInterval = Interval.open(3, 5)

    //iterate over all the types returned
    //searching for a type that matches
    //the worldCandyInterval
    val targetTypeOption = types.find(_.tokenInterval == worldCandyInterval)

    //assert that the worldCandyInterval has been matched
    //and that it has been tagged as a ''World Candy''
    assert(targetTypeOption.isDefined)
    assert(targetTypeOption.get.name === "WorldCandy")
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

    val taggerCollection = TaggerCollection.fromString(taggers)

    val testSentence = "I once saw the large cat on a couch ."

    val tokens = chunker.chunk(testSentence) map MorphaStemmer.lemmatizeToken

    val types = taggerCollection.tag(tokens)

    val typeTypes = types.filter(_.name == "TypeTaggerTest")
    assert(typeTypes.size === 3)
    assert(typeTypes.map(_.text).toSet == Set("the", "large", "cat"))

    val typeStartTypes = types.filter(_.name == "TypeStartTaggerTest")
    assert(typeStartTypes.size === 1)
    assert(typeStartTypes.headOption.map(_.text).get == "the")

    val typeEndTypes = types.filter(_.name == "TypeEndTaggerTest")
    assert(typeEndTypes.size === 1)
    assert(typeEndTypes.headOption.map(_.text).get == "cat")

    val typeEndANameTypes = types.filter(_.name == "TypeEndTaggerTest.aName")
    assert((typeEndANameTypes.size == 1))
    assert(typeEndANameTypes.headOption.map(_.text).get == "cat")
    
    val allNamedTypes = types.filter(p => p.isInstanceOf[NamedGroupType])
    assert((allNamedTypes.size ==1))
    assert((allNamedTypes.head.asInstanceOf[NamedGroupType].groupName == "aName"))

    val typeContTypes = types.filter(_.name == "TypeContTaggerTest")
    assert(typeContTypes.size === 2)
    assert(typeContTypes.headOption.map(_.text).get == "large")
  }
  
  
  "TypePatternTagger expressions" should "expand correctly" in {
    
    val taggers  =
      """VerbPhrase := PatternTagger{
    		<pos='VBD'> || <pos='VBZ'>
    	}
         TastyNounPhrase := PatternTagger{
    		<string='delicious'> <pos='NN'>
    	}
         TypePatternPhrase := TypePatternTagger{
    		@VerbPhrase @TastyNounPhrase <pos='RB'>
    	}
      """
      
    val taggerCollection = TaggerCollection.fromString(taggers)
    
    val testSentence = "James gives delicious candy frequently."
      
    val tokens = chunker.chunk(testSentence) map MorphaStemmer.lemmatizeToken

    val types = taggerCollection.tag(tokens)
    
    val typeTypes = types.filter(_.name == "TypePatternPhrase")
    assert(typeTypes.size === 1)
    
    val typesFromOverloadedTagMethod = taggerCollection.tag(testSentence)
    
    for((typ1,typ2) <- types.zip(typesFromOverloadedTagMethod)){
      assert(typ1.name == typ2.name)
      assert(typ1.text == typ2.text)
      assert(typ1.tokenInterval == typ2.tokenInterval)
    }
    
  }
}
