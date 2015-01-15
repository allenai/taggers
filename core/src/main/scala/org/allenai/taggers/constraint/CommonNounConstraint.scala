package org.allenai.taggers.constraint;

import org.allenai.nlpstack.core.repr.{Postags, Sentence}
import org.allenai.nlpstack.core.typer.Type

import java.util.regex.Pattern

object CommonNounConstraint extends Constraint[Sentence with Postags] {
  override def apply(sentence: Sentence with Postags, tag: Type): Boolean = {
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
