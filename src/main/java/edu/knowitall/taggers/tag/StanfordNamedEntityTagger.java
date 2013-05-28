package edu.knowitall.taggers.tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.knowitall.taggers.SentenceFunctions;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;
import edu.knowitall.tool.typer.StanfordNer;

/***
 * Tag Stanford named entities.
 * @author schmmd
 *
 */
public class StanfordNamedEntityTagger extends Tagger {
    protected static Logger logger = LoggerFactory.getLogger(StanfordNamedEntityTagger.class);
    public static final String DEFAULT_MODEL = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";

    StanfordNer typer = StanfordNer.withDefaultModel();

    public StanfordNamedEntityTagger(String descriptor) throws IOException {
        super(descriptor, "Stanford");
    }

    public List<Type> findTags(List<Lemmatized<ChunkedToken>> sentence) {
      List<edu.knowitall.tool.typer.Type> types = scala.collection.JavaConversions.asJavaList(typer.apply(scala.collection.JavaConversions.asScalaBuffer(SentenceFunctions.tokens(sentence))));
      List<Type> finalTypes = new ArrayList<Type>(types.size());
      for (edu.knowitall.tool.typer.Type type : types) {
          finalTypes.add(Type.fromSentence(sentence, type.name(), type.source(), type.tokenInterval()));
      }

      return finalTypes;
    }

    /// XML

    public StanfordNamedEntityTagger(Element e) throws ParseTagException, IOException {
        super(e.getAttributeValue("descriptor"), e.getAttributeValue("source"));

        String modelPath = e.getAttributeValue("model");
        if (modelPath == null) {
            // init(DEFAULT_MODEL);
        }
        else {
            throw new IllegalArgumentException("Unknown model: " + modelPath);
        }
    }

    @Override
    public void sort() {
        // TODO Auto-generated method stub

    }
}
