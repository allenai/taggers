package edu.knowitall.taggers.tag;

import java.util.List;

import org.jdom2.Element;

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
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTags(sentence);
        return AfterTaggers.tagChunks(keywordTags, sentence);
    }


    /// XML

    public CaseInsensitiveNpChunkTagger(Element e) throws ParseTagException {
        super(e);
    }
}
