package edu.knowitall.taggers.tag;

import java.util.List;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, ignoring case, and then tag the chunk
 * in which that keyword appears.
 * @author schmmd
 *
 */
public class CaseInsensitiveNpChunkTagger extends CaseInsensitiveKeywordTagger {
    public CaseInsensitiveNpChunkTagger(String descriptor, List<String> keywords) {
        super(descriptor, keywords);
    }

    public CaseInsensitiveNpChunkTagger(String descriptor, String keyword) {
        super(descriptor, keyword);
    }

    @Override
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTagsJava(sentence);
        return AfterTaggers.tagChunks(keywordTags, sentence);
    }
}
