package edu.knowitall.taggers.tag

import edu.knowitall.taggers.constraint.Constraint
import edu.knowitall.common.HashCodeHelper
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import collection.JavaConverters._
import edu.knowitall.tool.typer.Type
import edu.knowitall.tool.typer.Typer
import edu.knowitall.repr.sentence.Sentence

abstract class Tagger[-S <: Sentence] {
  type TheSentence = S

  def name: String
  def source: String

  def sort(): this.type = this

  override def toString = this.getClass.getName + ":=" + name
  override def equals(that: Any) = that match {
    // fast comparison for Intervals
    case that: Tagger[_] => that.canEqual(this) && that.name == this.name && that.source == this.source
    // slower comparison for Seqs
    case that: IndexedSeq[_] => super.equals(that)
    case _ => false
  }
  def canEqual(that: Any) = that.isInstanceOf[Tagger[_]]

  override def hashCode = HashCodeHelper(name /*, constraints*/ )

  def apply(sentence: S): Seq[Type] = this.tags(sentence)
  def apply(sentence: S, tags: Seq[Type]): Seq[Type] = this.tags(sentence, tags)

  /** Public method for finding tags in a sentence.
    * @param sentence
    * @return a list of the tags found
    */
  def tags(sentence: S): Seq[Type] = {
    tags(sentence, Seq.empty)
  }

  /** Public method for finding tags in a sentence with types.
    * This method also filters out types by constraint.
    *
    * @param sentence
    * @return a list of the tags found
    */
  def tags(sentence: S, types: Seq[Type]) = {
    var tags = findTagsWithTypes(sentence, types)

    // remove types that are covered by other types.
    tags = filterCovered(tags)
    tags = filterWithConstraints(sentence, tags)

    tags
  }

  def findTags(sentence: S): Seq[Type]

  /** This method should be overridden by any Tagger that wants to use the
    * Types accumulated from previous Taggers. If it's not overridden the sentence
    * will be tagged without type information.
    * @param sentence
    * @param types
    * @return
    */
  protected def findTagsWithTypes(sentence: S, types: Seq[Type]): Seq[Type] = {
    findTags(sentence)
  }

  /** Remove types that cover over types.
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

  /** Remove types that do not pass the constraints.
    */
  private def filterWithConstraints(sentence: S, types: Seq[Type]) = {
    for {
      tag <- types
      // if this.constraints.forall(_(sentence, tag))
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

  def create[S <: Sentence](classname: String, pack: String, name: String, args: Seq[String]): Tagger[S] = {
    create[S](getTaggerClass(classname, pack), Array[Class[_]](classOf[String], classOf[Seq[String]]), Array[Object](name, args))
  }

  def create[S <: Sentence](classname: String, pack: String, argTypes: Array[Class[_]], argValues: Array[Object]): Tagger[S] = {
    create[S](getTaggerClass(classname, pack), argTypes, argValues)
  }

  def create[S <: Sentence](tagger: Class[_], argTypes: Array[Class[_]], argValues: Array[Object]): Tagger[S] = {
    val constructor = tagger.getConstructor(argTypes: _*)
    constructor.newInstance(argValues: _*).asInstanceOf[Tagger[S]]
  }
}
