package edu.knowitall.taggers.constraint;

import java.util.List;
import java.util.regex.Pattern;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public class CommonNounConstraint extends Constraint {
    @Override
    public boolean apply(List<Lemmatized<ChunkedToken>> tokens, Type tag) {
        final Pattern commonNounPattern = Pattern.compile("NNS?", Pattern.CASE_INSENSITIVE);

        // make sure all tags are NN
        for (Lemmatized<ChunkedToken> token : tokens) {
            if (!commonNounPattern.matcher(token.token().postag()).matches()) {
                return false;
            }
        }

        return true;
    }
}
