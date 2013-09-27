package edu.knowitall.taggers.constraint;

import java.util.List;
import java.util.regex.Pattern;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

class CommonNounConstraint extends Constraint {
  override def apply(tokens: Seq[Lemmatized[ChunkedToken]], tag: Type): Boolean = {
    val commonNounPattern = Pattern.compile("NNS?", Pattern.CASE_INSENSITIVE)

    // make sure all tags are NN
    for (token <- tokens) {
      if (!commonNounPattern.matcher(token.token.postag).matches()) {
        return false
      }
    }

    return true
  }
}
