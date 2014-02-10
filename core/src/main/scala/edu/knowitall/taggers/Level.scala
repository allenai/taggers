package edu.knowitall.taggers

import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.tag.Tagger

case class Level[-S <: Sentence](taggers: Seq[Tagger[S]])
