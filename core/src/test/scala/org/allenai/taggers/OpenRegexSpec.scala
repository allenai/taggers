package org.allenai.taggers

import org.allenai.common.immutable.Interval
import org.allenai.nlpstack.chunk.OpenNlpChunker
import org.allenai.nlpstack.core.repr._
import org.allenai.nlpstack.lemmatize.MorphaStemmer
import org.allenai.nlpstack.postag.defaultPostagger
import org.allenai.nlpstack.tokenize.defaultTokenizer
import org.allenai.taggers.tag.Tagger

import org.scalatest.FlatSpec

class OpenRegexSpec extends FlatSpec {
  type MySentence = Tagger.Sentence with Chunks with Lemmas

  val chunker = new OpenNlpChunker();
  def makeSentence(text: String): MySentence =
    new Sentence(text) with Consume with Chunker with Lemmatizer {
      override val chunker = new OpenNlpChunker
      override val postagger = defaultPostagger
      override val tokenizer = defaultTokenizer
      override val lemmatizer = MorphaStemmer
    }

  "a OpenRegex with an empty group" should "create an empty subtype" in {
    val taggers = """
      DescribedNoun := OpenRegex {
        (<Description>:<pos='JJ'>*) (<Noun>:<pos='NN'>+)
      }
      """

    val cascade =
      new Cascade(Level.fromString[MySentence]("unnamed", taggers))

    val testSentence = "The huge fat cat lingered in the hallway."

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)

    // Description subtype will be empty when "hallway" is matched
    // because it uses `*`.
    types.exists(typ => typ.text.isEmpty)
  }

  "WorldCandy OpenRegex" should "match an occurrence from a sequence of Type Nationality and Type Candy" in {
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
        WorldCandy := OpenRegex {
          <type='Nationality'>+ <type='Candy'>+
        }
      """

    val cascade =
      new Cascade(Level.fromString[MySentence]("unnamed", taggers))

    //test sentence that should be tagged as
    // WorldCandy{(3,5)}
    val testSentence = "Vernon enjoys eating Argentinian Licorice."

    val s = makeSentence(testSentence)

    //Tag the sentence with the loaded taggers
    val (types, extractions) = cascade.apply(s)

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

  "expressions in OpenRegex" should "be able to span multiple lines" in {
    val taggers =
      """SimpleTagger := OpenRegex {
           // first line
           <string = 'a'>
           // second line
           <string = 'b'>
         }"""

    val cascade = new Cascade(Level.fromString[MySentence]("unnamed", taggers))

    val testSentence = "c a b c"

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)

    assert(types.size === 1)
  }

  "a TypedOpenRegex with undefined types" should "fail typechecking" in {
    val taggers =
      """NotTypesafe := TypedOpenRegex {
           @Asdf
         }"""

    intercept[IllegalArgumentException] {
      new Cascade(Level.fromString[MySentence]("unnamed", taggers))
    }
  }

  "cascades with OpenRegex" should "work correctly" in {
    val l0 =
      """consume TupleRelation := TypedOpenRegex {
        (?:<string="in"> <string="order"> <string="to">)
      }"""

    val l1 =
      """
      NP := OpenRegex {
        <chunk='B-NP'> <chunk='I-NP'>*
      }
      VG := TypedOpenRegex {
        <string="to">? <pos=/VB[DPZGN]?/> <pos=/R[PB]/>?
      }
      Tuple := TypedOpenRegex {
        (<Arg1>:@NP)? (<Rel>:@VG) (<Arg2>:@NP)?
      }
      RelatedTuples := TypedOpenRegex {
        (<Tuple1>:@Tuple) (<TupleRel>:@TupleRelation) (<Tuple2>:@Tuple)
      }"""

    val cascade = new Cascade(Seq(
      Level.fromString[MySentence]("unnamed", l0),
      Level.fromString[MySentence]("unnamed", l1)))

    val testSentence = "animals eat in order to get nutrients"

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)

    assert(types.exists(_.name == "RelatedTuples"), "RelatedTuples type not found.")
  }

  "the most recent consuming type and the subsequent types" should "be the only applicable types" in {
    val l0 =
      """consume A := OpenRegex {
        (?:<string="a">)
      }

      consume B := TypedOpenRegex {
        (?:@A <string="b">)
      }

      consume C := TypedOpenRegex {
        (?:@B <string="c">)
      }

      D := TypedOpenRegex {
        @A
      }

      E := TypedOpenRegex {
        @B
      }

      F := TypedOpenRegex {
        @C
      }

      G := TypedOpenRegex {
        @F
      }
      """

    val cascade = new Cascade(Seq(
      Level.fromString[MySentence]("unnamed", l0)))

    val testSentence = "a b c"

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)

    def exists(name: String) = {
      assert(types exists (_.name == name), s"type $name not found")
    }

    def notExists(name: String) = {
      assert(!(types exists (_.name == name)), s"type $name found")
    }

    exists("A")
    exists("B")
    exists("C")

    notExists("D")
    notExists("E")
    exists("F")

    exists("G")
  }

  "type fields in OpenRegex" should "match correctly" in {
    val taggers =
      """AnimalTagger := KeywordTagger{
           the large cat
         }
         TypeTaggerTest := OpenRegex {
           <type = 'AnimalTagger' >
         }
         TypeStartTaggerTest := OpenRegex {
           <typeStart = 'AnimalTagger' >
         }
         TypeContTaggerTest := OpenRegex {
           <typeCont = 'AnimalTagger' >
         }
         TypeEndTaggerTest := OpenRegex {
           (<aName>:<typeEnd = 'AnimalTagger' >)
         }
      """

    val cascade = new Cascade(Level.fromString[MySentence]("unnamed", taggers))

    val testSentence = "I once saw the large cat on a couch ."

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)

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


  "TypedOpenRegex expressions" should "expand correctly" in {

    val taggers  =
      """VerbPhrase := OpenRegex{
        <pos='VBD'> || <pos='VBZ'>
      }
      TastyNounPhrase := OpenRegex{
        <string='delicious'> <pos='NN'>
      }
      TypePatternPhrase := TypedOpenRegex{
        @VerbPhrase @TastyNounPhrase <pos='RB'>
      }
      """

    val cascade = new Cascade(Level.fromString[MySentence]("unnamed", taggers))

    val testSentence = "James gives delicious candy frequently."

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)

    val typeTypes = types.filter(_.name == "TypePatternPhrase")
    assert(typeTypes.size === 1)
  }

  "TypedOpenRegex expressions" should "match adjacent types seperately" in {
    val taggers  =
      """FemaleFirstName := KeywordTagger {
           mary
           jones
         }

         FirstName := TypedOpenRegex {
           (?:@FemaleFirstName)
         }"""

    val cascade = new Cascade(Level.fromString[MySentence]("unnamed", taggers))

    val testSentence = "mary jones."

    val s = makeSentence(testSentence)

    val (types, extractions) = cascade.apply(s)
    val filteredTypes = types.filter(_.name == "FirstName")

    assert(filteredTypes.size === 2)
  }
}
