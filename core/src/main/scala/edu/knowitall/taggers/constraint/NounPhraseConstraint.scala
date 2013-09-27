package edu.knowitall.taggers.constraint;

import java.util.List;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

class NounPhraseConstraint extends Constraint {
  override def apply(tokens: Seq[Lemmatized[ChunkedToken]], tag: Type): Boolean = {
    // make sure all chunk tags are NP
    for (token <- tokens) {
      if (!token.token.chunk.endsWith("NP")) {
        return false;
      }
    }

    return true;
  }
}