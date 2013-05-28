package edu.knowitall.taggers.tag;

import java.util.List;

import org.jdom2.Element;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for normalized keywords against a normalized sentence and tag the
 * chunk that contains the match if the match is the headword.  Normalized is
 * defined by {@see Stemmer.normalize()}.
 * @author schmmd
 *
 */
public class NormalizedNpHeadwordTagger extends NormalizedKeywordTagger {
    public NormalizedNpHeadwordTagger(String descriptor, List<String> keywords) {
        super(descriptor, keywords);
    }

    public NormalizedNpHeadwordTagger(String descriptor, String keyword) {
        super(descriptor, keyword);
    }

    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTags(sentence);
        return AfterTaggers.tagHeadword(keywordTags, sentence);
    }


    /// XML

    public NormalizedNpHeadwordTagger(Element e) throws ParseTagException {
        super(e);
    }
}
