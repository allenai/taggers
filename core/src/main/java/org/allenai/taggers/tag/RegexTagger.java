package org.allenai.taggers.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import edu.knowitall.collection.immutable.Interval;
import org.allenai.taggers.SentenceFunctions;
import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public class RegexTagger extends JavaTagger {
    public ImmutableList<Pattern[]> patterns;

    protected RegexTagger(String name) {
        super(name, null);
        this.patterns = null;
    }

    public RegexTagger(String name, List<String> keywords) {
        super(name, null);

        setPatterns(keywords);
    }

    /**
     * Constructor used by reflection.
     * @param name name of the tagger
     * @param args arguments to the tagger
     */
    public RegexTagger(String name, scala.collection.Seq<String> args) {
        this(name, scala.collection.JavaConversions.asJavaList(args));
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

    public RegexTagger(String name, String keyword) {
        this(name, Collections.singletonList(keyword));
    }

    @Override
    public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>>  sentence) {
        return this.findTagsJava(sentence, SentenceFunctions.strings(sentence));
    }

    protected List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence,
            List<String> tokens) {
        List<Type> tags = new ArrayList<Type>();
        for (Pattern[] pattern : this.patterns) {
            this.findTagsJava(tags, sentence, tokens, pattern);
        }

        return tags;
    }

    protected void findTagsJava(final List<Type> tags, final List<Lemmatized<ChunkedToken>>  sentence,
            List<String> tokens, final Pattern[] pattern) {

        for (int i = 0; i < tokens.size() - pattern.length; i++) {
            if (match(tokens, pattern, i)) {
                Interval range = Interval.open(i, i + pattern.length);
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
}
