package org.allenai.taggers.tag

import org.allenai.nlpstack.core.typer.Type
import org.allenai.taggers.pattern.TypedToken

import edu.knowitall.openregex
import edu.knowitall.openregex.Pattern.Group
import edu.knowitall.openregex.Pattern.Match

/** Run a token-based pattern over the text and tag matches.
  *
  * @author schmmd
  *
  */
class DebugOpenRegex(patternTaggerName: String, expression: String)
extends OpenRegex(patternTaggerName, expression) {
  def this(name: String, expressionLines: Seq[String]) {
    this(name, expressionLines.mkString(" "))
  }

  /** This is a helper method that creates the Type objects from a given
    * pattern and a List of TypedTokens.
    *
    * Matching groups will create a type with the name or index
    * appended to the name.
    *
    * The text used here provides information that is useful for debugging
    * patterns.
    *
    * @param typedTokenSentence
    * @param sentence
    * @param pattern
    * @return
    */
  override protected def findTags(typedTokenSentence: Seq[TypedToken],
    sentence: TheSentence,
    pattern: openregex.Pattern[TypedToken]) = {

    var tags = Seq.empty[Type]

    def matchString(m: Match[TypedToken]) = {
      def groupString(group: Group[TypedToken]) = {
        s"[${group.expr} : ${group.tokens.mkString(" ")}]"
      }
      s"""Match("${m.tokens.mkString(" ")}", ${m.pairs.map(groupString).mkString(", ")})"""
    }

    val matches = pattern.findAll(typedTokenSentence);
    for (m <- matches) {
      val group = m.groups.head

      val tokens = sentence.lemmatizedTokens.slice(group.interval.start, group.interval.end).map(_.token)
      val text = matchString(m)
      val tag = Type(this.name, this.source, OpenRegex.bridgeInterval(group.interval), text)
      tags = tags :+ tag
    }

    tags
  }
}
