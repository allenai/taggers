package edu.knowitall.taggers.constraint;

import java.util.List
import java.util.regex.Pattern
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Postags

object CommonNounConstraint extends Constraint[Sentence with Postags] {
  override def apply(sentence: TheSentence, tag: Type): Boolean = {
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
