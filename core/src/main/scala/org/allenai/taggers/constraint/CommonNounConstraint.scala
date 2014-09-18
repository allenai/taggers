package org.allenai.taggers.constraint;

import java.util.regex.Pattern

import org.allenai.nlpstack.core.repr.Postags
import org.allenai.nlpstack.core.repr.Sentence
import org.allenai.nlpstack.core.typer.Type

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
