package edu.knowitall.taggers.tag;

import java.util.List;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

abstract public class JavaTagger extends Tagger {
    private final String descriptor;
    private final String source;

    public String descriptor() {
        return descriptor;
    }

    public String source() {
        return source;
    }

    public JavaTagger(String descriptor, String source) {
        this.descriptor = descriptor;
        this.source = source;
    }

    @Override
    public scala.collection.Seq<Type> findTags(final scala.collection.Seq<Lemmatized<ChunkedToken>> sentence) {
        List<Lemmatized<ChunkedToken>> javaSentence = scala.collection.JavaConversions.seqAsJavaList(sentence);
        return scala.collection.JavaConversions.asScalaBuffer(this.findTagsJava(javaSentence)).toSeq();
    }

    abstract public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence);

    public Type createType(List<Lemmatized<ChunkedToken>> sentence, Interval interval) {
        return Type.fromSentence(sentence, this.descriptor, this.source, interval);
    }
}