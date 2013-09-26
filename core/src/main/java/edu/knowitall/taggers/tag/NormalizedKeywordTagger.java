package edu.knowitall.taggers.tag;

import java.util.Collections;
import java.util.List;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for normalized keywords against a normalized sentence and tag the
 * match.  Normalized is defined by {@see Stemmer.normalize()}.
 * @author schmmd
 *
 */
public class NormalizedKeywordTagger extends KeywordTagger {
    public NormalizedKeywordTagger(String name, List<String> keywords) {
        super(name, keywords);
    }

    public NormalizedKeywordTagger(String name, String keyword) {
        super(name, Collections.singletonList(keyword.toLowerCase()));
    }

    @Override
    protected List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        return super.findTagsJava(sentence, SentenceFunctions.lemmas(sentence));
    }
}
