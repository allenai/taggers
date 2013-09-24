package edu.knowitall.taggers.tag;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import com.google.common.collect.ImmutableList;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.collection.immutable.Interval$;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public class RegexTagger extends Tagger {
    public ImmutableList<Pattern[]> patterns;

    protected RegexTagger(String descriptor) {
        super(descriptor, null);
        patterns = null;
    }

    public RegexTagger(String descriptor, List<String> keywords) {
        super(descriptor, null);

        setPatterns(keywords);
    }

    private void setPatterns(List<String> expressions) {
        List<Pattern[]> split = new ArrayList<Pattern[]>();
        for (String expression : expressions) {
            String[] parts = expression.split("\\s+");

            Pattern[] patternParts = new Pattern[parts.length];
            for (int i = 0; i < parts.length; i++) {
                patternParts[i] = Pattern.compile(parts[i]);
            }

            split.add(patternParts);
        }

        this.patterns = ImmutableList.copyOf(split);
    }

    public RegexTagger(String descriptor, String keyword) {
        this(descriptor, Collections.singletonList(keyword));
    }

    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>>  sentence) {
        return this.findTags(sentence, SentenceFunctions.strings(sentence));
    }

    protected List<Type> findTags(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        List<Type> tags = new ArrayList<Type>();
        for (Pattern[] pattern : this.patterns) {
            this.findTags(tags, sentence, tokens, pattern);
        }


        return tags;
    }

    protected void findTags(final List<Type> tags, final List<Lemmatized<ChunkedToken>>  sentence,
            List<String> tokens, final Pattern[] pattern) {

        for (int i = 0; i < tokens.size() - pattern.length; i++) {
            if (match(tokens, pattern, i)) {
                Interval range = Interval$.MODULE$.open(i, i + pattern.length);
                tags.add(this.createType(sentence, range));
            }
        }
    }

    protected boolean match(List<String> source, Pattern[] target, int sourceIndex) {
        int targetIndex = 0;
        for (Pattern targetPattern : target) {
            String sourceWord = source.get(sourceIndex + targetIndex);

            if (!targetPattern.matcher(sourceWord).matches()) {
                return false;
            }

            targetIndex++;
        }

        return true;
    }


    /// XML

    @SuppressWarnings("unchecked")
    public RegexTagger(Element e) throws ParseTagException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        super(e);

        List<String> kw = new ArrayList<String>();
        Element keywords = e.getChild("patterns");
        if (keywords == null) {
            throw new ParseTagException("No element 'patterns'", e);
        }

        for (Element keyword : (List<Element>)keywords.getChildren("pattern")) {
            kw.add(keyword.getText());
        }

        this.setPatterns(kw);
    }

    public Element toXmlElement() {
        Element e = super.toXmlElement();

        Element patterns = new Element("patterns");
        for (Pattern[] keyword : this.patterns) {
            patterns.addContent(new Element("pattern").setText(
                    StringUtils.join(keyword, " ")));
        }

        e.addContent(patterns);

        return e;
    }

    @Override
    public void sort() {
    }
}
