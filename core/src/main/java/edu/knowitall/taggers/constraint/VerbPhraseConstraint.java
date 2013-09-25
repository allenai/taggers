package edu.knowitall.taggers.constraint;

import java.util.List;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public class VerbPhraseConstraint extends Constraint {
    @Override
    public boolean apply(List<Lemmatized<ChunkedToken>> tokens, Type tag) {
        // make sure at least one tag is VP
        // and no tags are NP
        boolean result = false;
        for (Lemmatized<ChunkedToken> token : tokens) {
            String chunkTag = token.token().chunk();
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
