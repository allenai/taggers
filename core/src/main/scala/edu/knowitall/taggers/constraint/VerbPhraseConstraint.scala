package edu.knowitall.taggers.constraint;

import java.util.List;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

class VerbPhraseConstraint extends Constraint {
  override def apply(tokens: Seq[Lemmatized[ChunkedToken]], tag: Type): Boolean = {
    // make sure at least one tag is VP
    // and no tags are NP
    var result = false;
    for (token <- tokens) {
      val chunkTag = token.token.chunk;
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
