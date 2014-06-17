package org.allenai.taggers.constraint;

import java.util.List
import org.allenai.nlpstack.typer.Type
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.repr.sentence.Sentence
import org.allenai.repr.sentence.Chunks

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
