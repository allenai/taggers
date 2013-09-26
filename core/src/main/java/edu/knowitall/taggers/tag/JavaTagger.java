package edu.knowitall.taggers.tag;

import java.util.List;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.taggers.TypeHelper;
import edu.knowitall.tool.stem.Lemmatized;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.typer.Type;

abstract public class JavaTagger extends Tagger {
    private final String name;
    private final String source;

    public String name() {
        return name;
    }

    public String source() {
        return source;
    }

    public JavaTagger(String name, String source) {
        this.name = name;
        this.source = source;
    }

    @Override
    public scala.collection.Seq<Type> findTags(final scala.collection.Seq<Lemmatized<ChunkedToken>> sentence) {
        List<Lemmatized<ChunkedToken>> javaSentence = scala.collection.JavaConversions.seqAsJavaList(sentence);
        return scala.collection.JavaConversions.asScalaBuffer(this.findTagsJava(javaSentence)).toSeq();
    }

    abstract public List<Type> findTagsJava(final List<Lemmatized<ChunkedToken>> sentence);

    public Type createType(List<Lemmatized<ChunkedToken>> sentence, Interval interval) {
        return TypeHelper.fromJavaSentence(sentence, this.name, this.source, interval);
    }
}
