package edu.knowitall.taggers.tag;

import java.util.List;

import org.jdom2.Element;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, ignoring case, and then tag the chunk
 * if the keyword is the headword of that chunk.
 * @author schmmd
 *
 */
public class CaseInsensitiveNpHeadwordTagger extends CaseInsensitiveKeywordTagger {
    public CaseInsensitiveNpHeadwordTagger(String descriptor, List<String> keywords) {
        super(descriptor, keywords);
    }

    public CaseInsensitiveNpHeadwordTagger(String descriptor, String keyword) {
        super(descriptor, keyword);
    }

    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTags(sentence);
        return AfterTaggers.tagHeadword(keywordTags, sentence);
    }


    /// XML

    public CaseInsensitiveNpHeadwordTagger(Element e) throws ParseTagException {
        super(e);
    }
}
