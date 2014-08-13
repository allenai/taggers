package org.allenai.taggers.tag

import org.allenai.nlpstack.core.typer.Type
import org.allenai.taggers.Consume

import edu.knowitall.common.HashCodeHelper

/** A tagger operations on a sentence to create types. */
abstract class Tagger[-S <: Tagger.Sentence] {
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

  /** Public method for finding tags in a sentence.
    * @param sentence
    * @return a list of the tags found
    */
  def apply(sentence: S): Seq[Type] = {
    this.apply(sentence, Seq.empty)
  }

  /** Public method for finding tags in a sentence with types.
    * This method also filters out types using the specified constraints.
    *
    * @param sentence
    * @param types  types already existing in the sentence
    * @param consumedIndices  indices used up on a previous level in a cascade
    * @return a list of the tags found
    */
  def apply(sentence: S, types: Seq[Type]): Seq[Type] = {
    var tags = tag(sentence, types)

    // remove types that are covered by other types.
    tags = filterCovered(tags)
    tags = filterWithConstraints(sentence, tags)

    tags
  }

  /** This method identifies the actual tags in the sentence.
    *
    * @param sentence
    * @param types  types already existing in the sentence
    *
    * @return
    */
  private[tag] def tag(sentence: S, types: Seq[Type]): Seq[Type]

  /** Typecheck the tagger against the defined types. */
  def typecheck(definedTypes: Set[String]): Unit = ()

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
  type Sentence = org.allenai.nlpstack.core.repr.Sentence with Consume

  def getTaggerClass(classname: String, pack: String): Class[_] = {
    try {
      Class.forName(pack + "." + classname.replace('.', '$'))
    } catch {
      case ex: ClassNotFoundException =>
        Class.forName(classname.replace('.', '$'))
    }
  }

  def create[S <: Tagger.Sentence](classname: String, pack: String, name: String, args: Seq[String]): Tagger[S] = {
    create[S](getTaggerClass(classname, pack), Array[Class[_]](classOf[String], classOf[Seq[String]]), Array[Object](name, args))
  }

  def create[S <: Tagger.Sentence](classname: String, pack: String, argTypes: Array[Class[_]], argValues: Array[Object]): Tagger[S] = {
    create[S](getTaggerClass(classname, pack), argTypes, argValues)
  }

  def create[S <: Tagger.Sentence](tagger: Class[_], argTypes: Array[Class[_]], argValues: Array[Object]): Tagger[S] = {
    val constructor = tagger.getConstructor(argTypes: _*)
    constructor.newInstance(argValues: _*).asInstanceOf[Tagger[S]]
  }
}
