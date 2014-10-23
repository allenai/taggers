package org.allenai.taggers.tag

import com.google.common.base.Function
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.allenai.common.immutable.Interval
import org.allenai.nlpstack.core.ChunkedToken
import org.allenai.nlpstack.core.Lemmatized
import org.allenai.nlpstack.core.typer.Type
import org.allenai.taggers.SentenceFunctions
import org.allenai.taggers.StringFunctions
import org.allenai.nlpstack.core.repr._

class KeywordTagger(val name: String, val wholeKeywords: Seq[String])
extends Tagger[Tagger.Sentence with Chunks] {
  override def source = null

  // Keywords are stored as a map for more efficient matching in a sentence.
  // The key of the map is the first token of the keyword.
  // The value of the map is a seq of keywords by token.
  private val keywordMap: Map[String, Seq[List[String]]] = {
    val keywords = wholeKeywords map (_.split("\\s+").toList)
    keywords.groupBy(_.take(1)).map { case (k, v) => k.head -> v }.toMap
  }

  override def equals(that: Any) = super.equals(that) && (that match {
    case that: KeywordTagger => wholeKeywords == that.wholeKeywords
    case _ => false
  })
  override def canEqual(that: Any) = that.isInstanceOf[KeywordTagger]

  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    keywordTag(sentence.strings, types)
  }

  /** This method exists so the sentence can be transformed before matching against the keywords. 
    * For example, the sentence might be lower cased so a case insensitive match can be done
    * against the keywords (which would already have been lower cased as well).
    */
  protected def keywordTag(sentence: Seq[String], types: Seq[Type]): Seq[Type] = {
    /** Try and match a keyword at a particular index. */
    def isMatch(target: Seq[String], sourceIndex: Int): Boolean = {
      var targetIndex = 0

      for (targetWord <- target) {
        if (sourceIndex + targetIndex >= sentence.size) {
          return false
        }

        val sourceWord = sentence(sourceIndex + targetIndex)

        if (!sourceWord.equals(targetWord)) {
          return false
        }

        targetIndex += 1
      }

      return true
    }

    /** Try and match a keyword at a particular index. 
      * If there is a match, create and return a Type.
      */
    def matchKeyword(keyword: Seq[String], i: Int): Option[Type] = {
      if (isMatch(keyword, i)) {
        val interval = Interval.open(i, i + keyword.length)
        val text = sentence.slice(interval.start, interval.end).mkString(" ")
        Some(Type(this.name, this.source, interval, text))
      }
      else {
        None
      }
    }

    for {
      // Iterate over the sentence tokens.
      i <- 0 until sentence.size
      // Iterate over any keyword where the first token matches the current token in the sentence.
      keyword <- keywordMap.get(sentence(i)).getOrElse(Seq.empty)
      // Check to see if there is a match with all the tokens in the keyword.
      typ <- matchKeyword(keyword, i)
    } yield typ
  }
}
