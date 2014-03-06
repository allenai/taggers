package org.allenai.taggers

import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.TokensSupertrait
import edu.knowitall.tool.typer.Type

/** We need a consuming trait that removes all information from
  * the sentence representation other than the consuming type.
  *
  * This is used to support cascades and the consume keyword.
  */
trait Consume {
  this: Sentence with TokensSupertrait =>
    private var backing: Array[Option[Type]] = null
    def consumingTypes: Array[Option[Type]] = {
      if (backing == null) {
        reset()
      }
      backing
    }

    /** Only return types that came after the consuming type.
      *
      * This includes the consuming type.
      */
    def availableTypes(i: Int, types: Seq[Type]) = {
      val typesHere = types filter (_.tokenInterval contains i)
      consumingTypes(i) match {
        case Some(consumer) =>
          typesHere dropWhile (_ != consumer)
        case None => typesHere
      }
    }

    /** Only return types that came before the consuming type. */
    def consumedTypes(i: Int, types: Seq[Type]) = {
      val typesHere = types filter (_.tokenInterval contains i)
      consumingTypes(i) match {
        case Some(consumer) =>
          typesHere takeWhile (_ != consumer)
        case None => typesHere
      }
    }

    private def reset() {
      backing = Array.fill(this.tokens.size)(None)
    }

    def consume(tag: Type) {
      for (i <- tag.tokenInterval) {
        backing(i) = Some(tag)
      }
    }
}
