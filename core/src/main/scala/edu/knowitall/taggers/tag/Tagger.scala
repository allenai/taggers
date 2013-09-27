package edu.knowitall.taggers.tag

import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.common.HashCodeHelper
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import collection.JavaConverters._
import edu.knowitall.tool.typer.Type

abstract class Tagger {
  def name: String
  def source: String
  var constraints: Seq[Constraint] = Seq.empty

  def sort(): this.type = this

  override def toString = this.getClass.getName + ":=" + name
  override def equals(that: Any): Boolean = {
    if (that == null) return false
    if (this == that) return true
    if (this.getClass() != that.getClass()) return false

    val tagger = that.asInstanceOf[Tagger]
    return this.name.equals(tagger.name)
  }

  override def hashCode = HashCodeHelper(name, constraints)

  def constrain(constraint: Constraint) {
    this.constraints :+= constraint
  }

  def apply(sentence: Seq[Lemmatized[ChunkedToken]]): Seq[Type] = this.tags(sentence)
  def apply(sentence: Seq[Lemmatized[ChunkedToken]], tags: Seq[Type]): Seq[Type] = this.tags(sentence, tags)

  /**
   * *
   * Public method for finding tags in a sentence.
   * @param sentence
   * @return a list of the tags found
   */
  def tags(sentence: Seq[Lemmatized[ChunkedToken]]): Seq[Type] = {
    tags(sentence, Seq.empty)
  }

  /**
   * *
   * Public method for finding tags in a sentence with types.
   * This method also filters out types by constraint.
   *
   * @param sentence
   * @return a list of the tags found
   */
  def tags(sentence: Seq[Lemmatized[ChunkedToken]], types: Seq[Type]) = {
    var tags = findTagsWithTypes(sentence, types)

    // remove types that are covered by other types.
    tags = filterCovered(tags)
    tags = filterWithConstraints(sentence, tags)

    tags
  }

  protected def findTags(sentence: Seq[Lemmatized[ChunkedToken]]): Seq[Type]

  /**
   * This method should be overridden by any Tagger that wants to use the
   * Types accumulated from previous Taggers. If it's not overridden the sentence
   * will be tagged without type information.
   * @param sentence
   * @param types
   * @return
   */
  protected def findTagsWithTypes(sentence: Seq[Lemmatized[ChunkedToken]], types: Seq[Type]): Seq[Type] = {
    findTags(sentence)
  }

  /**
   * Remove types that cover over types.
   * @param tags
   */
  private def filterCovered(tags: Seq[Type]): Seq[Type] = {
    tags.filter { tag =>
      tags.find { other =>
        other != tag &&
        other.name == tag.name &&
        other.source == tag.source &&
        (other.tokenInterval superset tag.tokenInterval)
      } match {
        case Some(superType) => false
        case None => true
      }
    }
  }

  /**
   * Remove types that do not pass the constraints.
   */
  private def filterWithConstraints(sentence: Seq[Lemmatized[ChunkedToken]], types: Seq[Type]) = {
    for {
      tag <- types
      if this.constraints.forall(_.apply(sentence, tag))
    } yield (tag)
  }
}

object Tagger {
  def getTaggerClass(classname: String, pack: String): Class[_] = {
    try {
      Class.forName(pack + "." + classname.replace('.', '$'))
    } catch {
      case ex: ClassNotFoundException =>
        Class.forName(classname.replace('.', '$'))
    }
  }

  def create(classname: String, pack: String, name: String, args: Seq[String]): Tagger = {
    create(getTaggerClass(classname, pack), Array[Class[_]](classOf[String], classOf[Seq[String]]), Array[Object](name, args))
  }

  def create(classname: String, pack: String, argTypes: Array[Class[_]], argValues: Array[Object]): Tagger = {
    create(getTaggerClass(classname, pack), argTypes, argValues)
  }

  def create(tagger: Class[_], argTypes: Array[Class[_]], argValues: Array[Object]): Tagger = {
    val constructor = tagger.getConstructor(argTypes :_*)
    constructor.newInstance(argValues :_*).asInstanceOf[Tagger]
  }
}