package edu.knowitall.taggers.example

import edu.knowitall.repr.sentence.Chunker
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.Cascade
import edu.knowitall.taggers.LinkedType
import edu.knowitall.taggers.NamedGroupType
import edu.knowitall.taggers.rule._
import edu.knowitall.taggers.Taggers
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer

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

  def process(text: String): Sentence with Chunks with Lemmas = {
    new Sentence(text) with Chunker with Lemmatizer {
      val chunker = Example.this.chunker
      val lemmatizer = MorphaStemmer
    }
  }

  def main(args: Array[String]) {

    val rules = new RuleParser[Sentence with Chunks with Lemmas].parse(pattern).get
    val cascade = new Cascade[Sentence with Chunks with Lemmas](Taggers.fromRules(rules))
    val lines = input.split("\n").map(f => f.trim()).filter(f => f != "").toList
    for (line <- lines) {
      val types = cascade.apply(process(line)).toList
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
