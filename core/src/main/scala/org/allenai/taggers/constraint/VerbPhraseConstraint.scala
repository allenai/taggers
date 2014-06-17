package org.allenai.taggers.constraint;

import java.util.List
import org.allenai.nlpstack.typer.Type
import org.allenai.nlpstack.chunk.ChunkedToken
import org.allenai.nlpstack.lemmatize.Lemmatized
import org.allenai.repr.sentence.Sentence
import org.allenai.repr.sentence.Chunks

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
