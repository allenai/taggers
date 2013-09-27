package edu.knowitall.taggers.tag;

import java.util.List;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, ignoring case, and then tag the chunk
 * in which that keyword appears.
 * @author schmmd
 *
 */
public class CaseInsensitiveNpChunkTagger extends CaseInsensitiveKeywordTagger {
    public CaseInsensitiveNpChunkTagger(String name, List<String> keywords) {
        super(name, keywords);
    }

    /**
     * Constructor used by reflection.
     * @param name name of the tagger
     * @param args arguments to the tagger
     */
    public CaseInsensitiveNpChunkTagger(String name, scala.collection.Seq<String> args) {
        this(name, scala.collection.JavaConversions.asJavaList(args));
    }

    @Override
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTagsJava(sentence);
        return AfterTaggers.tagChunks(keywordTags, sentence);
    }
}
