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

class KeywordTagger(val name: String, wholeKeywords: Seq[String])
extends Tagger[Tagger.Sentence with Chunks] {
  override def source = null

  var keywords = wholeKeywords map (_.split("\\s+"))

  override def equals(that: Any) = super.equals(that) && (that match {
    case that: KeywordTagger => keywords == that.keywords
    case _ => false
  })
  override def canEqual(that: Any) = that.isInstanceOf[KeywordTagger]

  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    keywordTag(sentence.strings, types)
  }

  protected def keywordTag(sentence: Seq[String], types: Seq[Type]): Seq[Type] = {
    def isMatch(target: Array[String], sourceIndex: Int): Boolean = {
      var targetIndex = 0

      for (targetWord <- target) {
        val sourceWord = sentence(sourceIndex + targetIndex)

        if (!sourceWord.equals(targetWord)) {
          return false
        }

        targetIndex += 1
      }

      return true
    }

    def findKeyword(keyword: Array[String]): Seq[Type] = {
      var types = Seq.empty[Type]

      for (i <- 0 to sentence.size - keyword.length) {
        if (isMatch(keyword, i)) {
          val interval = Interval.open(i, i + keyword.length)
          val text = sentence.slice(interval.start, interval.end).mkString(" ")
          types = types :+ Type(this.name, this.source, interval, text)
        }
      }

      types
    }

    for {
      keyword <- keywords
      typ <- findKeyword(keyword)
    } yield typ
  }
}
