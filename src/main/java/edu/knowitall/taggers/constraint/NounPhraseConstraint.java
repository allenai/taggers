package edu.knowitall.taggers.constraint;

import java.util.List;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public class NounPhraseConstraint extends Constraint {
    @Override
    public boolean apply(List<Lemmatized<ChunkedToken>> tokens, Type tag) {
        // make sure all chunk tags are NP
        for (Lemmatized<ChunkedToken> token : tokens) {
            if (!token.token().chunk().endsWith("NP")) {
                return false;
            }
        }

        return true;
    }
}