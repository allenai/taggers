package edu.knowitall.taggers.tag;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.StringFunctions;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, ignoring case.
 * @author schmmd
 *
 */
public class CaseInsensitiveKeywordTagger extends KeywordTagger {
    public CaseInsensitiveKeywordTagger(String descriptor, List<String> keywords) {
        super(descriptor, Lists
                .transform(keywords, StringFunctions.toLowerCase));
    }

    public CaseInsensitiveKeywordTagger(String descriptor, String keyword) {
        super(descriptor, Collections.singletonList(keyword.toLowerCase()));
    }

    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>> sentence) {
        return super.findTags(SentenceFunctions.lowercase(sentence));
    }


    /// XML

    public CaseInsensitiveKeywordTagger(Element e) throws ParseTagException {
        super(e);
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        super.setKeywords(Sets.newTreeSet(Iterables.transform(keywords, StringFunctions.toLowerCase)));
    }
}
