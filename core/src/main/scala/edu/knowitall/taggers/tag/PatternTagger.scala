package edu.knowitall.taggers.tag

import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set
import java.util.TreeMap
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.jdom2.Element
import com.google.common.base.Predicate
import com.google.common.collect.ImmutableList
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.washington.cs.knowitall.logic.ArgFactory
import edu.washington.cs.knowitall.logic.LogicExpression
import edu.washington.cs.knowitall.regex.Expression.BaseExpression
import edu.washington.cs.knowitall.regex.Expression.NamedGroup
import edu.washington.cs.knowitall.regex.ExpressionFactory
import edu.washington.cs.knowitall.regex.Match
import edu.washington.cs.knowitall.regex.RegularExpression
import edu.knowitall.openregex
import edu.knowitall.taggers.pattern.PatternBuilder
import edu.knowitall.taggers.pattern.TypedToken
import scala.collection.JavaConverters._
import edu.knowitall.taggers.TypeHelper

/**
 * *
 * Run a token-based pattern over the text and tag matches.
 *
 * @author schmmd
 *
 */
case class PatternTagger(name: String, expressions: Seq[String]) extends Tagger {
  override def source = null

  val patterns: Seq[openregex.Pattern[PatternBuilder.Token]] = this.compile(expressions)

  protected def this(name: String) {
    this(name, null: Seq[String])
  }

  private def compile(expressions: Seq[String]) = {
    expressions map PatternBuilder.compile
  }

  override def findTags(sentence: Seq[Lemmatized[ChunkedToken]]) = {
    this.findTagsWithTypes(sentence, Seq.empty[Type])
  }

  /**
   * This method overrides Tagger's default implementation. This
   * implementation uses information from the Types that have been assigned to
   * the sentence so far.
   */
  override def findTagsWithTypes(sentence: Seq[Lemmatized[ChunkedToken]],
    originalTags: Seq[Type]): Seq[Type] = {

    // create a java set of the original tags
    val originalTagSet = originalTags.toSet

    // convert tokens to TypedTokens
    val typedTokens = for ((token, i) <- sentence.zipWithIndex) yield {
      new TypedToken(token, i, originalTagSet.filter(_.tokenInterval contains i))
    }

    val tags = for {
      pattern <- patterns
      tag <- this.findTags(typedTokens, sentence, pattern)
    } yield (tag)

    return tags
  }

  /**
   * This is a helper method that creates the Type objects from a given
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
    sentence: Seq[Lemmatized[ChunkedToken]],
    pattern: openregex.Pattern[TypedToken]) = {

    var tags = Seq.empty[Type]

    val matches = pattern.findAll(typedTokenSentence);
    for (m <- matches) {
      val groupSize = m.groups.size
      for (i <- 0 until groupSize) {
        val group = m.groups(i);

        val postfix =
        group.expr match {
          case _ if i == 0 => ""
          case namedGroup: NamedGroup[_] => "." + namedGroup.name
          case _ => "." + i
        }
        val tag = TypeHelper.fromSentence(sentence, this.name + postfix,
          this.source, group.interval);
        tags = tags :+ tag
      }
    }

    tags
  }
}