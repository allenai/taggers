package edu.knowitall.taggers.tag;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.taggers.XmlSerializable;
import edu.knowitall.taggers.Type;
import edu.knowitall.taggers.constraint.*;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

/***
 * A class that searches over sentences and tags matching text with a
 * type.
 * @author schmmd
 *
 */
public abstract class Tagger implements XmlSerializable {
    public final /*interned*/ String descriptor;
    public final /*interned*/ String source;
    protected List<Constraint> constraints;

    public Tagger(String descriptor, String source) {
        this(descriptor, source, new ArrayList<Constraint>());
    }

    public Tagger(String descriptor, String source, List<Constraint> constraints) {
        this.descriptor = descriptor.intern();
        this.source = source == null ? null : source.intern();
        this.constraints = constraints;
    }

    public abstract void sort();

    @Override
    public boolean equals(Object that) {
        if (that == null) return false;
        if (this == that) return true;
        if (this.getClass() != that.getClass()) return false;

        Tagger tagger = (Tagger)that;
        return this.descriptor.equals(tagger.descriptor);
    }

    public abstract List<Type> findTags(List<Lemmatized<ChunkedToken>> sentence);
    
    /**
     * This method should be overridden by any Tagger that wants to use the
     * Types accumulated from previous Taggers. If it's not overridden the sentence
     * will be tagged without type information.
     * @param sentence
     * @param types
     * @return
     */
    public List<Type> getTags(List<Lemmatized<ChunkedToken>> sentence, List<Type> types){
    	return findTags(sentence);
    }

    /***
     * Remove types that cover over types.
     * @param tags
     */
    public void filter(List<Type> tags) {
        for (int i = 0; i < tags.size(); i++) {
            for (int j = 0; j < tags.size(); j++) {
                if (i != j) {
                    if (tags.get(i).interval().superset(tags.get(j).interval())) {
                        tags.set(j, tags.get(tags.size() - 1));
                        tags.remove(tags.size() - 1);

                        if (i >= tags.size()) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public Type createType(List<Lemmatized<ChunkedToken>> sentence, Interval interval) {
        return Type.fromSentence(sentence, this.descriptor, this.source, interval);
    }

    public void constrain(Constraint constraint) {
        this.constraints.add(constraint);
    }

    public static Class<?> getTaggerClass(String classname, String pack) throws ClassNotFoundException {
        return (Class<?>)Class.forName(pack + "." + classname.replace('.', '$'));
    }

    public static Tagger create(String classname, String pack, Class<?>[] argTypes, Object[] argValues) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return create(getTaggerClass(classname, pack), argTypes, argValues);
    }

    public static Tagger create(Class<?> tagger, Class<?>[] argTypes, Object[] argValues) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> constructor = tagger.getConstructor(argTypes);
        return (Tagger)constructor.newInstance(argValues);
    }

    public static Tagger create(Element element) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String classname = element.getName();

        String pack = element.getAttributeValue("package");
        if (pack == null) {
            pack = Tagger.class.getPackage().getName();
        }

        return Tagger.create(classname, pack, new Class[] { Element.class }, new Object[] { element });
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.descriptor + ")";
    }

    /// XML

    /***
     * Deserialize from an XML element.
     */
    public Tagger(Element e) throws ParseTagException {
        this.descriptor = e.getAttributeValue("descriptor");
        this.source = e.getAttributeValue("source");
        if (this.descriptor == null) {
            throw new ParseTagException("No attribute 'descriptor'", e);
        }

        List<Constraint> constraints = new ArrayList<Constraint>();
        @SuppressWarnings("unchecked")
        List<Element> constraintElements = (List<Element>)e.getChildren("constraint");
        for (Element element : constraintElements) {
            String type = element.getAttributeValue("type");
            constraints.add(Constraint.create(type));
        }

        this.constraints = constraints;
    }

    /***
     * Serialize to an XML element.
     */
    public Element toXmlElement() {
        String nameWithoutPackage = this.getClass().getName().substring(this.getClass().getPackage().getName().length() + 1).replace('$', '.');
        Element e = new Element(nameWithoutPackage);

        if (!this.getClass().getPackage().getName().equals(Tagger.class.getPackage().getName())) {
            e.setAttribute("package", this.getClass().getPackage().getName());
        }

        e.setAttribute("descriptor", this.descriptor);

        for (Constraint constraint : constraints) {
            e.addContent(new Element("constraint").setAttribute("type", constraint.getClass().getSimpleName()));
        }

        return e;
    }
}
