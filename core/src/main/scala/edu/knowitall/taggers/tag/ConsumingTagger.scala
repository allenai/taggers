package edu.knowitall.taggers.tag

import edu.knowitall.tool.typer.Type

class ConsumingTagger[S <: Tagger.Sentence](val tagger: Tagger[S])
    extends Tagger[S] {
  override def name = tagger.name
  override def source = tagger.source

  def findTags(sentence: TheSentence): Seq[Type] = {
    tagger.findTags(sentence)
  }
}
