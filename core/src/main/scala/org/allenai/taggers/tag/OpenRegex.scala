package org.allenai.taggers.tag

import com.google.common.base.Predicate
import com.google.common.collect.ImmutableList
import edu.knowitall.openregex
import edu.washington.cs.knowitall.logic.ArgFactory
import edu.washington.cs.knowitall.logic.LogicExpression
import edu.washington.cs.knowitall.regex.Expression.BaseExpression
import edu.washington.cs.knowitall.regex.Expression.NamedGroup
import edu.washington.cs.knowitall.regex.ExpressionFactory
import edu.washington.cs.knowitall.regex.Match
import edu.washington.cs.knowitall.regex.RegularExpression
import org.allenai.common.immutable.Interval
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.nlpstack.tokenize.Tokenizer
import org.allenai.nlpstack.typer.Type
import org.allenai.repr.sentence
import org.allenai.repr.sentence.Chunks
import org.allenai.repr.sentence.Lemmas
import org.allenai.repr.sentence.Sentence
import org.allenai.taggers.LinkedType
import org.allenai.taggers.NamedGroupType
import org.allenai.taggers.pattern.PatternBuilder
import org.allenai.taggers.pattern.TypedToken
import org.allenai.taggers.TypeHelper

import java.util.regex.Matcher
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import scala.util.control._

/** Run a token-based pattern over the text and tag matches.
  *
  * @author schmmd
  *
  */
class OpenRegex(patternTaggerName: String, expression: String) extends Tagger[Tagger.Sentence with Chunks with Lemmas] {
  override def name = patternTaggerName
  override def source = null

  val pattern: openregex.Pattern[PatternBuilder.Token] =
    try {
      PatternBuilder.compile(expression)
    }
    catch {
      case NonFatal(e) =>
        throw new OpenRegex.OpenRegexException(s"Could not compile pattern for $patternTaggerName.", e)
    }

  /** The constructor used by reflection.
    *
    * Multiple lines are collapsed to create a single expression.
    */
  def this(name: String, expressionLines: Seq[String]) {
    this(name, expressionLines.mkString(" "))
  }

  /** This method overrides Tagger's default implementation. This
    * implementation uses information from the Types that have been assigned to
    * the sentence so far.
    */
  override def tag(sentence: TheSentence,
    originalTags: Seq[Type]): Seq[Type] = {

    // convert tokens to TypedTokens
    val typedTokens = OpenRegex.buildTypedTokens(sentence, originalTags)

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
        val text = tokens match {
          case head +: _ => Tokenizer.originalText(tokens, head.offsets.start)
          case Seq() => ""
        }
        val tag = group.expr match {
          // create the main type for the group
          case _ if i == 0 =>
            val typ = Type(this.name, this.source, OpenRegex.bridgeInterval(group.interval), text)
            parent = Some(typ) // There may be children of this type.
            Some(typ)
          case namedGroup: NamedGroup[_] =>
            require(parent.isDefined)
            val name = this.name + "." + namedGroup.name
            Some(new NamedGroupType(namedGroup.name, Type(name, this.source, OpenRegex.bridgeInterval(group.interval), text), parent))
          case _ => None
        }

        tag.foreach { t =>
          tags = tags :+ t
        }
      }
    }

    tags
  }
}

object OpenRegex {
  class OpenRegexException(message: String, cause: Throwable)
  extends Exception(message, cause)

  def bridgeInterval(interval: edu.knowitall.collection.immutable.Interval): org.allenai.common.immutable.Interval = {
    Interval.open(interval.start, interval.end)
  }

  def buildTypedTokens(sentence: Tagger.Sentence with Chunks with Lemmas, types: Seq[Type]) = {
    for ((token, i) <- sentence.lemmatizedTokens.zipWithIndex) yield {
      val availableTypes = sentence.availableTypes(i, types)
      val consumed = sentence.consumingTypes(i).isDefined
      new TypedToken(token, i, availableTypes.toSet, consumed)
    }
  }
}
