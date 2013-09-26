package edu.knowitall.taggers.tag;

import java.util.List;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, ignoring case, and then tag the chunk
 * if the keyword is the headword of that chunk.
 * @author schmmd
 *
 */
public class CaseInsensitiveNpHeadwordTagger extends CaseInsensitiveKeywordTagger {
    public CaseInsensitiveNpHeadwordTagger(String name, List<String> keywords) {
        super(name, keywords);
    }

    public CaseInsensitiveNpHeadwordTagger(String name, String keyword) {
        super(name, keyword);
    }

    @Override
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>>  sentence) {
        List<Type> keywordTags = super.findTagsJava(sentence);
        return AfterTaggers.tagHeadword(keywordTags, sentence);
    }
}
