package edu.knowitall.taggers.tag;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.google.common.collect.Iterables;

import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.chunk.OpenNlpChunker;
import edu.knowitall.tool.stem.Lemmatized;
import edu.knowitall.tool.stem.MorphaStemmer;
import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.Type;

/***
 * A collection of {@see Tagger}s.
 * @author schmmd
 *
 */
public class TaggerCollection {
    private static final Logger logger = LoggerFactory.getLogger(TaggerCollection.class);

    private final List<Tagger> taggers;

    public TaggerCollection() {
        this(Collections.<Tagger> emptyList());
    }

    public TaggerCollection(Collection<Tagger> taggers) {
        this.taggers = new ArrayList<Tagger>(taggers);
    }

    public TaggerCollection(Tagger tagger) {
        this(Arrays.asList(new Tagger[] {tagger}));
    }

    @Override
    public String toString() {
        return this.taggers.toString();
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) return false;
        if (this == that) return true;
        if (this.getClass() != that.getClass()) return false;

        TaggerCollection col = (TaggerCollection)that;
        return Iterables.elementsEqual(taggers, col.taggers);
    }

    public void sort() {
        Collections.sort(this.taggers, new Comparator<Tagger>() {
            @Override
            public int compare(Tagger o1, Tagger o2) {
                return o1.descriptor.compareTo(o2.descriptor);
            }});

        for (Tagger tagger : taggers) {
            tagger.sort();
        }
    }

    public void addTagger(Tagger tagger) {
        this.taggers.add(tagger);
    }

    public List<Type> tag(List<Lemmatized<ChunkedToken>> sentence) {
        ArrayList<Type> list = new ArrayList<Type>();
        for (Tagger tagger : taggers) {
            list.addAll(tagger.getTags(sentence,new ArrayList<Type>(list)));
        }

        return list;
    }

    // XML

    public TaggerCollection(Element e) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        @SuppressWarnings("unchecked")
        List<Element> children = (List<Element>) e.getChildren();

        this.taggers = new ArrayList<Tagger>();

        for (Element child : children) {
            logger.debug("create tagger: " + child.getAttributeValue("descriptor"));
            this.taggers.add(Tagger.create(child));
        }
    }

    public TaggerCollection(Document d) throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException {
        this(d.getRootElement());
    }

    public static TaggerCollection fromPath(String path) throws SecurityException, IllegalArgumentException, JDOMException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        File file = new File(path);
        if (file.isDirectory()) {
            logger.info("Loading taggers from directory: " + file);
            return fromDirectory(file);
        }
        else {
            return fromFile(file);
        }
    }

    public static TaggerCollection fromFile(File file) throws JDOMException, IOException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        logger.info("loading from: " + file.getAbsolutePath());

        Document doc = new SAXBuilder().build(file);
        if (doc.getRootElement().getName().equals("taggers")) {
            // tagger collection file
            return new TaggerCollection(doc);
        }
        else if (doc.getRootElement().getName().equals("tagger")) {
            // single tagger file
            return new TaggerCollection(Tagger.create(doc.getRootElement()));
        }
        throw new IllegalArgumentException("Could not create tagger from file: " + file.toString());
    }

    public static TaggerCollection fromDirectory(File directory) throws JDOMException, IOException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Directory required.");
        }

        List<Tagger> taggers = new ArrayList<Tagger>();

        File[] files = directory.listFiles();
        Arrays.sort(files);

        for (File file : files) {
            if (file.isDirectory()) {
                taggers.addAll(fromDirectory(file).taggers);
            }
            else if (file.isFile()) {
                if (file.getName().endsWith(".xml")) {
                    logger.info("Loading taggers from file: " + file);
                    taggers.addAll(fromFile(file).taggers);
                }
            }
        }

        return new TaggerCollection(taggers);
    }

    public Element toXmlElement() {
        Element e = new Element("taggers");
        for (Tagger tagger : taggers) {
            e.addContent(tagger.toXmlElement());
        }

        return e;
    }

    public static void main(String[] args) throws SecurityException, IllegalArgumentException, JDOMException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        TaggerCollection taggers = TaggerCollection.fromPath(args[0]);

        OpenNlpChunker chunker = new OpenNlpChunker();

        System.out.println("Please enter a sentence to tag:");

        MorphaStemmer morpha = new MorphaStemmer();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            List<ChunkedToken> chunkedSentence = scala.collection.JavaConverters.seqAsJavaListConverter(chunker.chunk(line)).asJava();
            List<Lemmatized<ChunkedToken>> tokens = new ArrayList<Lemmatized<ChunkedToken>>(chunkedSentence.size());
            for (ChunkedToken token : chunkedSentence) {
                Lemmatized<ChunkedToken> lemma = morpha.lemmatizeToken(token);
                tokens.add(lemma);
            }

            System.out.println(SentenceFunctions.text(tokens));

            List<Type> types = taggers.tag(tokens);
            for (Type type : types) {
                System.out.println(type);
            }
        }
    }
}
