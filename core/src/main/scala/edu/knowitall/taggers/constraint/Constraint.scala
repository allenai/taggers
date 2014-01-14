package edu.knowitall.taggers.constraint;

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.List
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized;
import edu.knowitall.repr.sentence.Sentence

abstract class Constraint[-S <: Sentence] {
  type TheSentence = S
  def apply(sentence: S, tag: Type): Boolean
}

object Constraint {
  def create[S <: Sentence](classname: String): Constraint[S] = {
    create[S](classname, Array.empty[Class[_]], Array.empty[Object])
  }

  def create[S <: Sentence](tagger: Class[_], argTypes: Array[Class[_]], argValues: Array[Object]): Constraint[S] = {
    try {
      val constructor = tagger.getConstructor(argTypes: _*)
      constructor.newInstance(argValues).asInstanceOf[Constraint[S]]
    } catch {
      case e: Exception =>
        throw new IllegalArgumentException("Could not create class: " + tagger.getName(), e);
    }
  }

  def getConstraintClass(classname: String): Class[_] = {
    Class.forName(classOf[Constraint[_]].getPackage().getName() + "." + classname)
  }

  def create[S <: Sentence](classname: String, argTypes: Array[Class[_]], argValues: Array[Object]): Constraint[S] = {
    create(getConstraintClass(classname), argTypes, argValues);
  }
}
