package org.allenai.taggers.tag

import com.google.common.base.Function
import com.google.common.collect.Lists
import org.allenai.nlpstack.core.ChunkedToken
import org.allenai.nlpstack.core.Lemmatized
import org.allenai.nlpstack.core.typer.Type
import org.allenai.nlpstack.lemmatize.MorphaStemmer

/** Search for normalized keywords against a normalized sentence and tag the
  * match.  Lemmatized is defined by {@see Stemmer.lemmatize()}.
  * @author schmmd
  */
class CaseInsensitiveKeywordTagger(name: String, keywords: Seq[String])
extends KeywordTagger(name, keywords map (_.toLowerCase)) {
  override def canEqual(that: Any) = that.isInstanceOf[CaseInsensitiveKeywordTagger]

  override def tag(sentence: TheSentence, types: Seq[Type]): Seq[Type] = {
    keywordTag(sentence.strings map (_.toLowerCase), types)
  }
}
