package edu.knowitall.taggers.tag;

import java.util.List;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for normalized keywords against a normalized sentence and tag the
 * chunk that contains the match if the match is the headword.  Lemmatized is
 * defined by {@see Stemmer.lemmatize()}.
 * @author schmmd
 *
 */
public class LemmatizedNpHeadwordTagger extends LemmatizedKeywordTagger {
    public LemmatizedNpHeadwordTagger(String name, List<String> keywords) {
        super(name, keywords);
    }

    /**
     * Constructor used by reflection.
     * @param name name of the tagger
     * @param args arguments to the tagger
     */
    public LemmatizedNpHeadwordTagger(String name, scala.collection.Seq<String> args) {
        this(name, scala.collection.JavaConversions.asJavaList(args));
    }

    @Override
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTagsJava(sentence);
        return AfterTaggers.tagHeadword(keywordTags, sentence);
    }
}
