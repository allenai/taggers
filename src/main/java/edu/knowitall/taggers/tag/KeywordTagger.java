package edu.knowitall.taggers.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.collection.immutable.Interval$;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.StringFunctions;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, case-sensitive, and tag the match.
 * @author schmmd
 *
 */
public class KeywordTagger extends Tagger {
    public ImmutableList<String[]> keywords;

    protected KeywordTagger(String descriptor) {
        super(descriptor, null);
        keywords = null;
    }

    public KeywordTagger(String descriptor, List<String> keywords) {
        super(descriptor, null);

        setKeywords(new TreeSet<String>(keywords));
    }

    public KeywordTagger(String descriptor, String keyword) {
        this(descriptor, Collections.singletonList(keyword));
    }

    protected void setKeywords(Set<String> keywords) {
        this.keywords = ImmutableList.copyOf(
                Iterables.transform(keywords, StringFunctions.split("\\s+")));
    }

    protected void setKeywords(Iterable<String> keywords) {
        this.keywords = ImmutableList.copyOf(
                Iterables.transform(keywords, StringFunctions.split("\\s+")));
    }

    protected List<String> getKeywords() {
        return Lists.transform(this.keywords, new Function<String[], String>() {
            @Override
            public String apply(String[] parts) {
                return Joiner.on(" ").join(parts);
            }
        });
    }

    @Override
    public void sort() {
        List<String> keywords = this.getKeywords();
        Collections.sort(keywords);
        this.setKeywords(keywords);
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) return false;
        if (this == that) return true;
        if (this.getClass() != that.getClass()) return false;

        KeywordTagger kt = (KeywordTagger)that;
        if (!super.equals(that)) {
            return false;
        }

        if (this.keywords.size() != kt.keywords.size()) {
            return false;
        }

        if (this.keywords.size() != kt.keywords.size()) {
            return false;
        }

        Iterator<String[]> i1 = this.keywords.iterator();
        Iterator<String[]> i2 = kt.keywords.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            if (!Arrays.equals(i1.next(), i2.next())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>> sentence) {
        return this.findTags(sentence, SentenceFunctions.strings(sentence));
    }

    protected List<Type> findTags(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        List<Type> tags = new ArrayList<Type>();
        for (String[] keyword : keywords) {
            this.findTags(tags, sentence, tokens, keyword);
        }
        return tags;
    }

    protected void findTags(final List<Type> tags, final List<Lemmatized<ChunkedToken>>  sentence,
            List<String> tokens, final String[] keyword) {

        for (int i = 0; i < tokens.size() - keyword.length; i++) {
            if (match(tokens, keyword, i)) {
                Interval range = Interval$.MODULE$.open(i, i + keyword.length);
                tags.add(this.createType(sentence, range));
            }
        }
    }

    protected boolean match(List<String> source, String[] target, int sourceIndex) {
        int targetIndex = 0;
        for (String targetWord : target) {
            String sourceWord = source.get(sourceIndex + targetIndex);

            if (!sourceWord.equals(targetWord)) {
                return false;
            }

            targetIndex++;
        }

        return true;
    }


    /// XML

    @SuppressWarnings("unchecked")
    public KeywordTagger(Element e) throws ParseTagException {
        super(e);

        List<String> kw = new ArrayList<String>();
        Element keywords = e.getChild("keywords");
        if (keywords == null) {
            throw new ParseTagException("No element 'keywords'", e);
        }

        for (Element keyword : (List<Element>)keywords.getChildren("keyword")) {
            kw.add(keyword.getText());
        }

        this.setKeywords(new TreeSet<String>(kw));
    }

    public Element toXmlElement() {
        Element e = super.toXmlElement();

        Element keywords = new Element("keywords");
        for (String[] keyword : this.keywords) {
            keywords.addContent(new Element("keyword").setText(
                    StringUtils.join(keyword, " ")));
        }

        e.addContent(keywords);

        return e;
    }
}
