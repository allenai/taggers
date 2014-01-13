package edu.knowitall.taggers.example

import edu.knowitall.taggers.TaggerCollection
import edu.knowitall.taggers.LinkedType
import edu.knowitall.taggers.NamedGroupType
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.taggers.ParseRule

object Example {

  val pattern = """
    Animal := NormalizedKeywordTagger {
      cat
      kitten
      dog
      puppy
    }
    Color := NormalizedKeywordTagger{
      blue
      red
      yellow
      green
    }
    ColorfulAnimal := PatternTagger {
      //namedGroup color will yield a Type object
      //that is linked to the ColorfulAnimal Type object
      (<color>:<type='Color'>) <type='Animal'>
    }
    ColorfulAnimalAction := TypePatternTagger{
      //TypePatternTagger supports @ syntax to capture
      // the entire Type
      @ColorfulAnimal <pos='VBD'>
    }
    """

  val input = """
    I have a red dog.
    Cliff has a yellow puppy.
    The yellow puppy ran.
    """

  val chunker = new OpenNlpChunker()

  def process(text: String): Sentence with Chunked with Lemmatized = {
    new Sentence(text) with Chunker with Lemmatizer {
      val chunker = Example.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def main(args: Array[String]) {

    val rules = new ParseRule[Sentence with Chunked with Lemmatized].parse(pattern).get
    val t = rules.foldLeft(new TaggerCollection[Sentence with Chunked with Lemmatized]()) { case (ctc, rule) => ctc + rule }
    val lines = input.split("\n").map(f => f.trim()).filter(f => f != "").toList
    for (line <- lines) {
      val types = t.tag(process(line)).toList
      println("Line: " + line)
      for (typ <- types) {
        println("TaggerName: " + typ.name + "\tTypeInterval: " + typ.tokenInterval + "\t TypeText: " + typ.text)
      }
      //filter out the NamedGroupTypes
      for (typ <- types.filter(p => p.isInstanceOf[NamedGroupType])) {
        val namedGroupType = typ.asInstanceOf[NamedGroupType]
        if (namedGroupType.groupName == "color") {
          println("COLOR:\t" + namedGroupType.text)
        }
      }
    }
  }
}