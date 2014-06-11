package org.allenai.taggers.tag

import edu.knowitall.openregex
import org.allenai.repr.sentence.Sentence
import org.allenai.taggers.Cascade
import org.allenai.taggers.constraint.Constraint
import org.allenai.taggers.pattern.PatternBuilder
import org.allenai.taggers.pattern.TypedToken
import org.allenai.taggers.TypeHelper
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.nlpstack.typer.Type
import edu.washington.cs.knowitall.regex.Expression.NamedGroup

import scala.util.matching.Regex

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
