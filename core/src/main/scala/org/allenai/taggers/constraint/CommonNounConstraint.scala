package org.allenai.taggers.constraint;

import java.util.List
import java.util.regex.Pattern
import org.allenai.nlpstack.typer.Type
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.repr.sentence.Sentence
import org.allenai.repr.sentence.Postags

object CommonNounConstraint extends Constraint[Sentence with Postags] {
  override def apply(sentence: TheSentence, tag: Type): Boolean = {
    val commonNounPattern = Pattern.compile("NNS?", Pattern.CASE_INSENSITIVE)

    // make sure all tags are NN
    for (token <- sentence.tokens.slice(tag.tokenInterval.start, tag.tokenInterval.end)) {
      if (!commonNounPattern.matcher(token.postag).matches()) {
        return false
      }
    }

    return true
  }
}
