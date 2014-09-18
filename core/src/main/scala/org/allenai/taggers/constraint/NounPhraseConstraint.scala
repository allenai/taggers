package org.allenai.taggers.constraint;

import org.allenai.nlpstack.core.repr.Chunks
import org.allenai.nlpstack.core.repr.Sentence
import org.allenai.nlpstack.core.typer.Type

object NounPhraseConstraint extends Constraint[Sentence with Chunks] {
  override def apply(sentence: TheSentence, tag: Type): Boolean = {
    // make sure all chunk tags are NP
    for (token <- sentence.tokens.slice(tag.tokenInterval.start, tag.tokenInterval.end)) {
      if (!token.chunk.endsWith("NP")) {
        return false
      }
    }

    return true
  }
}
