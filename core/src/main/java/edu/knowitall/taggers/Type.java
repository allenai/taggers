package edu.knowitall.taggers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Element;

import com.google.common.base.Joiner;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.collection.immutable.Interval$;
import edu.knowitall.taggers.XmlSerializable;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public class Type implements Serializable, XmlSerializable, Comparable<Type> {
    private static final long serialVersionUID = -7792154752479518756L;

    private final Interval interval;
    private final /*interned*/ String descriptor;
    private final /*interned*/ String source;
    private final String match;
    private final String text;

    public Type(String text, String descriptor, String source, String match, Interval interval) {
        this.descriptor = descriptor.intern();
        this.source = source == null ? null : source.intern();
        this.interval = interval;
        this.text = text;
        this.match = match;
    }

    public Type(List<String> tokens, String descriptor, String source, String match,
            Interval interval) {
        this(Joiner.on(" ").join(tokens), descriptor, source, match, interval);
    }

    public Type(String[] tokens, String descriptor, String source, String match, Interval interval) {
        this(Arrays.asList(tokens), descriptor, source, null, interval);
    }

    public static Type fromSentence(List<Lemmatized<ChunkedToken>> sentence, String descriptor, String source, String match,
            Interval interval) {
        // build tyep string from tokens
        StringBuilder builder = new StringBuilder();
        for (Lemmatized<ChunkedToken> token : sentence.subList(interval.start(), interval.end())) {
            builder.append(token.token().string() + " ");
        }

        return new Type(builder.toString().trim(), descriptor, source, match, interval);
    }


    public static Type fromSentence(List<Lemmatized<ChunkedToken>> sentence, String descriptor, String source,
            Interval interval) {
        return fromSentence(sentence, descriptor, source, null, interval);
    }

    /***
     * The interval of tokens this tag spans.  A type always starts and ends on
     * token boundaries.  This is important for using types in regular expressions,
     * for example.
     * @return
     */
    public Interval interval() {
        return this.interval;
    }

    /***
     * A name for this type.
     * @return
     */
    public String descriptor() {
        return this.descriptor;
    }

    /***
     * @return a string representation of where this type came from.  For example,
     * if it were identified by the Stanford NER, you might use "Stanford".
     */
    public String source() {
        return this.source;
    }

    /***
     * @return the text that represents the tokens this type spans.
     */
    public String text() {
        return this.text;
    }

    /***
     * @return the text this type matches.  This may differ from
     * text().  For example, you may have a tagger that tags
     * "5-year-old" as an age, but it wants to use the text "5".  In
     * this case, "5" would be the match but "5-year-old" would be
     * the text.
     */
    public String match() {
        if (this.match == null) {
            return this.text();
        }
        else {
            return this.match;
        }
    }

    public String toString() {
        return descriptor + "{" + this.interval().toString() + ":" + this.text() + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Type)) {
            return false;
        }

        Type otherTag = (Type)other;


        // descriptor and source are interned so == is a valid comparison
        return this.descriptor == otherTag.descriptor
            && this.source == otherTag.source
            && this.interval.equals(otherTag.interval());
    }

    @Override
    public int hashCode() {
        int hash;

        hash = this.descriptor.hashCode();
        hash = 31 * hash + this.interval.hashCode();

        return hash;
    }

    /// XML
    public static Type fromXmlElement(List<Lemmatized<ChunkedToken>> sentence, Element e) {
        Interval interval = Interval$.MODULE$.open(
                Integer.parseInt(e.getAttributeValue("start")),
                Integer.parseInt(e.getAttributeValue("end")));
        return Type.fromSentence(sentence, e.getAttributeValue("descriptor"), e.getAttributeValue("source"), e.getAttributeValue("match"), interval);
    }

    @Override
    public Element toXmlElement() {
        Element e = new Element("type");
        e.setAttribute("descriptor", this.descriptor);
        if (this.source != null) {
            e.setAttribute("source", this.source);
        }
        if (this.match != null) {
            e.setAttribute("match", this.match);
        }
        e.setAttribute("start", Integer.toString(this.interval().start()));
        e.setAttribute("end", Integer.toString(this.interval().end()));
        e.setAttribute("text", this.text());
        return e;
    }

    @Override
    public int compareTo(Type type) {
        if (this.interval().leftOf(type.interval())) {
            return -1;
        }
        else if (this.interval().rightOf(type.interval())) {
            return +1;
        }
        else {
            return this.descriptor.compareTo(type.descriptor());
        }
    }
}
