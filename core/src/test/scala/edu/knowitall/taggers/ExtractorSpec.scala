package edu.knowitall.taggers

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.tag.Tagger
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.stem.MorphaStemmer

import org.scalatest.FlatSpec

class ExtractorSpec extends FlatSpec {
  type MySentence = Tagger.Sentence with sentence.Chunks with sentence.Lemmas

  val chunker = new OpenNlpChunker();
  def makeSentence(text: String): MySentence =
    new Sentence(text) with sentence.Chunker with sentence.Lemmatizer with Consume {
      override val chunker = new OpenNlpChunker
      override val lemmatizer = MorphaStemmer
    }

  "Extractor helper methods" should "work correctly" in {
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
      Level.fromString[MySentence]("l0", l0),
      Level.fromString[MySentence]("l1", l1)))

    val testSentence = "animals eat in order to get nutrients"
    val s = makeSentence(testSentence)
    val (types, _) = cascade.apply(s)

    val relatedTuplesTuple1Type = (types find (_.name == "RelatedTuples.Tuple1")).get
    val alignedTypes = Extractor.findAlignedTypes(types)(relatedTuplesTuple1Type)
    assert(alignedTypes.exists(_.name == "Tuple"))

    val namedAlignedTypes = Extractor.findAlignedTypesWithName(types)(relatedTuplesTuple1Type, "Tuple")
    assert(namedAlignedTypes.size === 1)

    val relatedTuplesType = (types find (_.name == "RelatedTuples")).get
    val subtypes = Extractor.findSubtypes(types)(relatedTuplesType)
    assert(subtypes.size === 3)

    val tuple1Subtypes = Extractor.findSubtypesWithName(types)(relatedTuplesType, "Tuple1")
    assert(tuple1Subtypes.size === 1)

    val parsed = new ExtractorParser().parse("x: RelatedTuples => (${x.Tuple1->Tuple.Arg1|'None'}, ${x.Tuple1->Tuple.Rel}, ${x.Tuple1->Tuple.Arg2|'None'}) --${x.TupleRel}-> (${x.Tuple2->Tuple.Arg1|'None'}, ${x.Tuple2->Tuple.Rel}, ${x.Tuple2->Tuple.Arg2|'None'})").get
    assert(parsed(types).head === "(animals, eat, None) --in order to-> (None, get, nutrients)")

    // This is a rather bogus extractor to test that when the Arg2 is missing
    // the Arg1 is used instead.
    val parsed2 = new ExtractorParser().parse("x: RelatedTuples => (${x.Tuple1->Tuple.Arg1|x.Tuple1->Tuple.Arg2}, ${x.Tuple1->Tuple.Rel}, ${x.Tuple1->Tuple.Arg2|x.Tuple1->Tuple.Arg1}) --${x.TupleRel}-> (${x.Tuple2->Tuple.Arg1|'None'}, ${x.Tuple2->Tuple.Rel}, ${x.Tuple2->Tuple.Arg2|'None'})").get
    assert(parsed2(types).head === "(animals, eat, animals) --in order to-> (None, get, nutrients)")

    // This is another rather bogus extractor to test that when the Arg2 is missing,
    // it tries again and then gives up and uses a string.
    val parsed3 = new ExtractorParser().parse("x: RelatedTuples => (${x.Tuple1->Tuple.Arg1|x.Tuple1->Tuple.Arg2}, ${x.Tuple1->Tuple.Rel}, ${x.Tuple1->Tuple.Arg2|x.Tuple1->Tuple.Arg2|'asdf'}) --${x.TupleRel}-> (${x.Tuple2->Tuple.Arg1|'None'}, ${x.Tuple2->Tuple.Rel}, ${x.Tuple2->Tuple.Arg2|'None'})").get
    assert(parsed3(types).head === "(animals, eat, asdf) --in order to-> (None, get, nutrients)")
  }

  "Extractor" should "be parsed correctly" in {
    val extractorDefinition = "x: RelatedTuples => (${x.Tuple1->Tuple.Arg1} !)"
    val parsed = new ExtractorParser().parse(extractorDefinition).get
    assert(parsed.toString === extractorDefinition)
    assert(parsed.targetType === "RelatedTuples")
    assert(parsed.parts.size === 3)
  }

  "Extractor with leading substitution" should "be parsed correctly" in {
    val parsed = new ExtractorParser().parse("x: RelatedTuples => ${x.Tuple1->Tuple.Arg1}").get
    assert(parsed.targetType === "RelatedTuples")
    assert(parsed.parts.size === 1)
  }
}
