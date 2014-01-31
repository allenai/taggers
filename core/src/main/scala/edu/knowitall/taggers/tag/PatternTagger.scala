package edu.knowitall.taggers.tag

import com.google.common.base.Predicate
import com.google.common.collect.ImmutableList
import edu.knowitall.openregex
import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.Chunks
import edu.knowitall.repr.sentence.Lemmas
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.taggers.LinkedType
import edu.knowitall.taggers.NamedGroupType
import edu.knowitall.taggers.pattern.PatternBuilder
import edu.knowitall.taggers.pattern.TypedToken
import edu.knowitall.taggers.TypeHelper
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.tokenize.Tokenizer
import edu.knowitall.tool.typer.Type
import edu.washington.cs.knowitall.logic.ArgFactory
import edu.washington.cs.knowitall.logic.LogicExpression
import edu.washington.cs.knowitall.regex.Expression.BaseExpression
import edu.washington.cs.knowitall.regex.Expression.NamedGroup
import edu.washington.cs.knowitall.regex.ExpressionFactory
import edu.washington.cs.knowitall.regex.Match
import edu.washington.cs.knowitall.regex.RegularExpression

import java.util.regex.Matcher
import java.util.regex.Pattern
import scala.collection.JavaConverters._

/** Run a token-based pattern over the text and tag matches.
  *
  * @author schmmd
  *
  */
class PatternTagger(patternTaggerName: String, expression: String) extends Tagger[Sentence with Chunks with Lemmas] {
  override def name = patternTaggerName
  override def source = null

  val pattern: openregex.Pattern[PatternBuilder.Token] = PatternBuilder.compile(expression)

  /** The constructor used by reflection.
    *
    * Multiple lines are collapsed to create a single expression.
    */
  def this(name: String, expressionLines: Seq[String]) {
    this(name, expressionLines.mkString(" "))
  }

  override def findTags(sentence: TheSentence) = {
    this.findTagsWithTypes(sentence, Seq.empty[Type], Seq.empty[Int])
  }

  /** This method overrides Tagger's default implementation. This
    * implementation uses information from the Types that have been assigned to
    * the sentence so far.
    */
  override def findTagsWithTypes(sentence: TheSentence,
    originalTags: Seq[Type], consumedIndices: Seq[Int]): Seq[Type] = {

    val originalTagSet = originalTags.toSet

    // convert tokens to TypedTokens
    val typedTokens = PatternTagger.buildTypedTokens(sentence, originalTagSet, consumedIndices)

    val tags = for {
      tag <- this.findTags(typedTokens, sentence, pattern)
    } yield (tag)

    return tags
  }

  /** This is a helper method that creates the Type objects from a given
    * pattern and a List of TypedTokens.
    *
    * Matching groups will create a type with the name or index
    * appended to the name.
    *
    * @param typedTokenSentence
    * @param sentence
    * @param pattern
    * @return
    */
  protected def findTags(typedTokenSentence: Seq[TypedToken],
    sentence: TheSentence,
    pattern: openregex.Pattern[TypedToken]) = {

    var tags = Seq.empty[Type]

    val matches = pattern.findAll(typedTokenSentence);
    for (m <- matches) {
      val groupSize = m.groups.size
      var parent: Option[Type] = None
      for (i <- 0 until groupSize) {
        val group = m.groups(i);

        val tokens = sentence.lemmatizedTokens.slice(group.interval.start, group.interval.end).map(_.token)
        val text = Tokenizer.originalText(tokens, tokens.head.offsets.start)
        val tag = group.expr match {
          // create the main type for the group
          case _ if i == 0 =>
            val typ = Type(this.name, this.source, group.interval, text)
            parent = Some(typ) // There may be children of this type.
            typ
          case namedGroup: NamedGroup[_] =>
            require(parent.isDefined)
            val name = this.name + "." + namedGroup.name
            new NamedGroupType(namedGroup.name, Type(name, this.source, group.interval, text), parent)
          case _ =>
            require(parent.isDefined)
            val name = this.name + "." + i
            new LinkedType(Type(name, this.source, group.interval, text), parent)
        }
        tags = tags :+ tag
      }
    }

    tags
  }
}

object PatternTagger {
  def buildTypedTokens(sentence: Sentence with Chunks with Lemmas, types: Set[Type], consumedIndices: Seq[Int] = Seq.empty) = {
    for ((token, i) <- sentence.lemmatizedTokens.zipWithIndex) yield {
      new TypedToken(token, i, types filter (_.tokenInterval contains i), consumedIndices contains i)
    }
  }
}
