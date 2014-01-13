package edu.knowitall.taggers

import scala.collection.JavaConverters._
import org.scalatest.FlatSpec
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence

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

    val opennlpChunker = new OpenNlpChunker

    val taggerCollection =
      TaggerCollection.fromString[Sentence with sentence.Chunked with sentence.Lemmatized](taggers)

    //test sentence that should be tagged as
    // WorldCandy{(3,5)}
    val testSentence = "Vernon enjoys eating Argentinian Licorice."

    val s = new Sentence(testSentence) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    //Tag the sentence with the loaded taggers
    val types = taggerCollection.tag(s)

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

  "expressions in PatternTagger" should "be able to span multiple lines" in {
    val taggers =
      """SimpleTagger := PatternTagger {
           // first line
           <string = 'a'>
           // second line
           <string = 'b'>
         }"""

    val opennlpChunker = new OpenNlpChunker

    val taggerCollection = TaggerCollection.fromString[Sentence with sentence.Chunked with sentence.Lemmatizer](taggers)

    val testSentence = "c a b c"

    val s = new Sentence(testSentence) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    val types = taggerCollection.tag(s)

    assert(types.size === 1)
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

    val opennlpChunker = new OpenNlpChunker

    val taggerCollection = TaggerCollection.fromString[Sentence with sentence.Chunked with sentence.Lemmatizer](taggers)

    val testSentence = "I once saw the large cat on a couch ."

    val s = new Sentence(testSentence) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    val types = taggerCollection.tag(s)

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

    val opennlpChunker = new OpenNlpChunker

    val taggerCollection = TaggerCollection.fromString[Sentence with sentence.Chunked with sentence.Lemmatized](taggers)

    val testSentence = "James gives delicious candy frequently."

    val s = new Sentence(testSentence) with sentence.Chunker with sentence.Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

    val types = taggerCollection.tag(s)

    val typeTypes = types.filter(_.name == "TypePatternPhrase")
    assert(typeTypes.size === 1)
  }
}
