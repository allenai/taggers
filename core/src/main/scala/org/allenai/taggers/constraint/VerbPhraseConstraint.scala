package org.allenai.taggers.constraint;

import org.allenai.nlpstack.core.repr.Chunks
import org.allenai.nlpstack.core.repr.Sentence
import org.allenai.nlpstack.core.typer.Type

object VerbPhraseConstraint extends Constraint[Sentence with Chunks] {
  override def apply(sentence: TheSentence, tag: Type): Boolean = {
    // make sure at least one tag is VP
    // and no tags are NP
    var result = false;
    for (token <- sentence.tokens.slice(tag.tokenInterval.start, tag.tokenInterval.end)) {
      val chunkTag = token.chunk;
      if (chunkTag.endsWith("NP")) {
        return false;
      }
      if (chunkTag.endsWith("VP")) {
        result = true;
      }
    }
    return result;
  }
}
