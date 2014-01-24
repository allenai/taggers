package edu.knowitall.taggers.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.StringFunctions;
import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * Search for exact keyword matches, case-sensitive, and tag the match.
 * @author schmmd
 *
 */
public class KeywordTagger extends JavaTagger {
    public ImmutableList<String[]> keywords;

    public KeywordTagger(String name, Collection<String> keywords) {
        super(name, null);

        setKeywords(new TreeSet<String>(keywords));
    }

    /**
     * Constructor used by reflection.
     * @param name name of the tagger
     * @param args arguments to the tagger
     */
    public KeywordTagger(String name, scala.collection.Seq<String> args) {
        this(name, scala.collection.JavaConversions.asJavaList(args));
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
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence) {
        return this.findTagsJava(sentence, SentenceFunctions.strings(sentence));
    }

    protected List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        List<Type> tags = new ArrayList<Type>();
        for (String[] keyword : keywords) {
            this.findTagsJava(tags, sentence, tokens, keyword);
        }
        return tags;
    }

    protected void findTagsJava(final List<Type> tags, final List<Lemmatized<ChunkedToken>>  sentence,
            List<String> tokens, final String[] keyword) {

        for (int i = 0; i <= tokens.size() - keyword.length; i++) {
            if (match(tokens, keyword, i)) {
                Interval range = Interval.open(i, i + keyword.length);
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
}
