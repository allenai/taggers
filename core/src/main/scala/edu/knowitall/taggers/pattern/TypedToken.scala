package edu.knowitall.taggers.pattern

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.typer.Type

/** A representation of a token that includes information about the
  * types at that token.
  *
  * @param types  a Type Set that stores all of the intersecting types at a given Token.
  * @param consumed  true if this token was consumed by a past level
  */
case class TypedToken(token: Lemmatized[ChunkedToken], index: Int, types: Set[Type], consumed: Boolean) {
  override def toString = token.token.string

  /** a Type Set that stores all of the types with the same ending offset as the Token
    */
  lazy val typesBeginningAtToken = {
    types.filter(_.tokenInterval.start == this.index)
  }

  /** a Type Set that stores all of the types with the same beginning offset as the Token
    */
  lazy val typesEndingAtToken = {
    types.filter(_.tokenInterval.last == this.index)
  }
  lazy val typesContinuingAtToken = types -- typesBeginningAtToken
}
