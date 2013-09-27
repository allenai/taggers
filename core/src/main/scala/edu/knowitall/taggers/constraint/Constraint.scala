package edu.knowitall.taggers.constraint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import edu.knowitall.tool.typer.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;

abstract class Constraint {
  def apply(sentence: Seq[Lemmatized[ChunkedToken]], tag: Type): Boolean
}

object Constraint {
  def create(classname: String): Constraint = {
    create(classname, Array.empty[Class[_]], Array.empty[Object])
  }

  def create(tagger: Class[_], argTypes: Array[Class[_]], argValues: Array[Object]): Constraint = {
    try {
      val constructor = tagger.getConstructor(argTypes: _*)
      constructor.newInstance(argValues).asInstanceOf[Constraint]
    } catch {
      case e: Exception =>
        throw new IllegalArgumentException("Could not create class: " + tagger.getName(), e);
    }
  }

  def getConstraintClass(classname: String): Class[_] = {
    Class.forName(classOf[Constraint].getPackage().getName() + "." + classname)
  }

  def create(classname: String, argTypes: Array[Class[_]], argValues: Array[Object]): Constraint = {
    create(getConstraintClass(classname), argTypes, argValues);
  }
}