package edu.knowitall.taggers.tag;

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

    /**
     * Constructor used by reflection.
     * @param name name of the tagger
     * @param args arguments to the tagger
     */
    public NormalizedKeywordTagger(String name, scala.collection.Seq<String> args) {
        this(name, scala.collection.JavaConversions.asJavaList(args));
    }

    @Override
    protected List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        return super.findTagsJava(sentence, SentenceFunctions.lemmas(sentence));
    }
}
