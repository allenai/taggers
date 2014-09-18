package org.allenai.taggers.tag

import org.allenai.nlpstack.core.typer.Type
import org.allenai.taggers.constraint.Constraint

class ConstrainedTagger[S <: Tagger.Sentence](val tagger: Tagger[S], val constraints: Seq[Constraint[S]])
    extends Tagger[S] {
  override def name = tagger.name
  override def source = tagger.source

  def constrain[SS <: S](constraint: Constraint[SS]) =
    new ConstrainedTagger[SS](tagger, constraints :+ constraint)

  override def tag(sentence: S, types: Seq[Type]): Seq[Type] = {
    tagger.tag(sentence, types).filter(tag => constraints.forall(_.apply(sentence, tag)))
  }
}
