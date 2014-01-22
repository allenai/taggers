package edu.knowitall.taggers.constraint;

import java.util.List
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Chunks

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
