package edu.knowitall.taggers.constraint;

import java.util.List
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Chunked

object NounPhraseConstraint extends Constraint[Sentence with Chunked] {
  override def apply(sentence: TheSentence, tag: Type): Boolean = {
    // make sure all chunk tags are NP
    for (token <- sentence.tokens.slice(tag.tokenInterval.start, tag.tokenInterval.end)) {
      if (!token.chunk.endsWith("NP")) {
        return false;
      }
    }

    return true;
  }
}
