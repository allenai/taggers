package edu.knowitall.taggers.tag;

import java.util.List;
import com.google.common.collect.Lists;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.StringFunctions;
import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, ignoring case.
 * @author schmmd
 *
 */
public class CaseInsensitiveKeywordTagger extends KeywordTagger {
    public CaseInsensitiveKeywordTagger(String name, List<String> keywords) {
        super(name, Lists
                .transform(keywords, StringFunctions.toLowerCase));
    }

    /**
     * Constructor used by reflection.
     * @param name name of the tagger
     * @param args arguments to the tagger
     */
    public CaseInsensitiveKeywordTagger(String name, scala.collection.Seq<String> args) {
        this(name, scala.collection.JavaConversions.asJavaList(args));
    }

    @Override
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence) {
        return super.findTagsJava(SentenceFunctions.lowercase(sentence));
    }
}
