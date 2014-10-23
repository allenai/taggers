package org.allenai.taggers.tag

import com.google.common.base.Function
import com.google.common.collect.Lists
import org.allenai.nlpstack.core.ChunkedToken
import org.allenai.nlpstack.core.typer.Type
import org.allenai.nlpstack.lemmatize.MorphaStemmer

/** Search for lowercase keywords against a lowercase sentence and tag the
  * match.  CaseInsensitive is defined by {@see Stemmer.lemmatize()}.
  * @author schmmd
  */
class CaseInsensitiveNpChunkKeywordTagger(name: String, keywords: Seq[String])
extends NpChunkKeywordTagger(name, keywords map MorphaStemmer.lemmatize) {
  override def canEqual(that: Any) = that.isInstanceOf[CaseInsensitiveKeywordTagger]

  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    keywordTag(sentence.strings map MorphaStemmer.lemmatize, types)
  }
}
