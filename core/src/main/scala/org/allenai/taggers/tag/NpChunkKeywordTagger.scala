package org.allenai.taggers.tag

import org.allenai.nlpstack.core.typer.Type
import org.allenai.common.immutable.Interval
import org.allenai.nlpstack.core.repr._

class NpChunkKeywordTagger(name: String, wholeKeywords: Seq[String])
extends KeywordTagger(name, wholeKeywords) {
  override def canEqual(that: Any) = that.isInstanceOf[NpChunkKeywordTagger]

  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    for {
      typ <- keywordTag(sentence.strings, types)
      npInterval <- NpChunkKeywordTagger.npChunkIntervals(sentence.chunks).find { interval =>
        interval.superset(typ.tokenInterval)
      }
    } yield {
      val text = sentence.strings.slice(npInterval.start, npInterval.end).mkString(" ")
      Type(typ.name, typ.source, npInterval, text)
    }
  }
}

object NpChunkKeywordTagger {
  def npChunkIntervals(chunks: Seq[String]): Seq[Interval] = {
    var start = -1
    var i = 0

    var intervals = Seq.empty[Interval]
    for (chunk <- chunks) {
      // end a chunk tag sequence
      if (start != -1 && !chunk.equalsIgnoreCase("I-NP")) {
        intervals = intervals :+ Interval.open(start, i)
        start = -1
      }

      // start a chunk tag sequence
      if (chunk.equalsIgnoreCase("B-NP")) {
        start = i
      }

      i = i + 1
    }

    intervals
  }
}
