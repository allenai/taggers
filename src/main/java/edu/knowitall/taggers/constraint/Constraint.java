package edu.knowitall.taggers.constraint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import edu.knowitall.taggers.tag.ParseTagException;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

public abstract class Constraint {
    public abstract boolean apply(List<Lemmatized<ChunkedToken>> sentence, Type tag);

    public static Class<?> getConstraintClass(String classname) throws ClassNotFoundException {
        return (Class<?>)Class.forName(Constraint.class.getPackage().getName() + "." + classname);
    }

    public static Constraint create(String classname, Class<?>[] argTypes, Object[] argValues) throws ParseTagException {
        try {
            return create(getConstraintClass(classname), argTypes, argValues);
        } catch (ClassNotFoundException e) {
            throw new ParseTagException(e);
        }
    }

    public static Constraint create(Class<?> tagger, Class<?>[] argTypes, Object[] argValues) throws ParseTagException {
            try {
                Constructor<?> constructor = tagger.getConstructor(argTypes);
                return (Constraint)constructor.newInstance(argValues);
            } catch (SecurityException e) {
                throw new ParseTagException("Could not create class: " + tagger.getName(), e);
            } catch (NoSuchMethodException e) {
                throw new ParseTagException("Could not create class: " + tagger.getName(), e);
            } catch (InstantiationException e) {
                throw new ParseTagException("Could not create class: " + tagger.getName(), e);
            } catch (IllegalAccessException e) {
                throw new ParseTagException("Could not create class: " + tagger.getName(), e);
            } catch (InvocationTargetException e) {
                throw new ParseTagException("Could not create class: " + tagger.getName(), e);
            }
    }

    public static Constraint create(String classname) throws ParseTagException {
        return create(classname, new Class<?>[] {}, new Object[] {});
    }
}
