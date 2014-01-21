package edu.knowitall.taggers.tag

import edu.knowitall.openregex
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.Cascade
import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.taggers.pattern.PatternBuilder
import edu.knowitall.taggers.pattern.TypedToken
import edu.knowitall.taggers.TypeHelper
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.typer.Type
import edu.washington.cs.knowitall.regex.Expression.NamedGroup

import scala.util.matching.Regex

class ConstrainedTagger[S <: Sentence](val tagger: Tagger[S], val constraints: Seq[Constraint[S]])
    extends Tagger[S] {
  override def name = tagger.name
  override def source = tagger.source

  def constrain[SS <: S](constraint: Constraint[SS]) =
    new ConstrainedTagger[SS](tagger, constraints :+ constraint)

  def findTags(sentence: TheSentence): Seq[Type] = {
    tagger.findTags(sentence).filter(tag => constraints.forall(_.apply(sentence, tag)))
  }
}
