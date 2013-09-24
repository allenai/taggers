package edu.knowitall.taggers.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;

import com.google.common.collect.ImmutableList;

import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;
import edu.washington.cs.knowitall.morpha.MorphaStemmer;

/***
 * Search for normalized keywords against a normalized sentence and tag the
 * match.  Normalized is defined by {@see Stemmer.normalize()}.
 * @author schmmd
 *
 */
public class NormalizedKeywordTagger extends KeywordTagger {
    public NormalizedKeywordTagger(String descriptor, List<String> keywords) {
        super(descriptor, keywords);
    }

    public NormalizedKeywordTagger(String descriptor, String keyword) {
        super(descriptor, Collections.singletonList(keyword.toLowerCase()));
    }

    @Override
    protected List<Type> findTags(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        return super.findTags(sentence, SentenceFunctions.lemmas(sentence));
    }


    /// XML

    public NormalizedKeywordTagger(Element e) throws ParseTagException {
        super(e);
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        List<String[]> transformedKeywords = new ArrayList<String[]>(keywords.size());
        for (String keyword : keywords) {
            String[] tokens = keyword.split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = MorphaStemmer.stem(tokens[i]);
            }

            transformedKeywords.add(tokens);
        }

        this.keywords = ImmutableList.copyOf(transformedKeywords);
    }
}
