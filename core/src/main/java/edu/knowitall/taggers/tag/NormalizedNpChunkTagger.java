package edu.knowitall.taggers.tag;

import java.util.List;

import org.jdom2.Element;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for normalized keywords against a normalized sentence and tag the
 * chunk that contains the match.  Normalized is defined by
 * {@see Stemmer.normalize()}.
 * @author schmmd
 *
 */
public class NormalizedNpChunkTagger extends NormalizedKeywordTagger {
    public NormalizedNpChunkTagger(String descriptor, List<String> keywords) {
        super(descriptor, keywords);
    }

    public NormalizedNpChunkTagger(String descriptor, String keyword) {
        super(descriptor, keyword);
    }

    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTags(sentence);
        return AfterTaggers.tagChunks(keywordTags, sentence);
    }


    /// XML

    public NormalizedNpChunkTagger(Element e) throws ParseTagException {
        super(e);
    }
}
