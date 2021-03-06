package org.allenai.taggers.pattern

object Fields {
  abstract class Field {
    def field(token: PatternBuilder.Token): Iterable[String]
    def apply(token: PatternBuilder.Token) = field(token)
  }
  /** The string portion of a token.
    *
    * @author schmmd
    */
  object StringField extends Field {
    override def field(token: PatternBuilder.Token): Iterable[String] =
      Seq(token.token.token.string)
  }

  /** The lemma portion of a token.
    *
    * @author schmmd
    */
  object LemmaField extends Field {
    override def field(token: PatternBuilder.Token): Iterable[String] =
      Seq(token.token.lemma)
  }

  /** The postag portion of a token.
    *
    * @author schmmd
    */
  object PostagField extends Field {
    override def field(token: PatternBuilder.Token): Iterable[String] =
      Seq(token.token.token.postag)
  }

  /** The chunk portion of a token.
    *
    * @author schmmd
    */
  object ChunkField extends Field {
    override def field(token: PatternBuilder.Token): Iterable[String] =
      Seq(token.token.token.chunk)
  }

  abstract class AbstractTypeField extends Field

  object TypeField extends AbstractTypeField {
    override def field(token: PatternBuilder.Token): Iterable[String] = {
      token.types.map(_.name)
    }
  }

  object TypeStartField extends AbstractTypeField {
    override def field(token: PatternBuilder.Token): Iterable[String] = {
      token.typesBeginningAtToken.map(_.name)
    }
  }

  object TypeContField extends AbstractTypeField {
    override def field(token: PatternBuilder.Token): Iterable[String] = {
      token.typesContinuingAtToken.map(_.name)
    }
  }

  object TypeEndField extends AbstractTypeField {
    override def field(token: PatternBuilder.Token): Iterable[String] = {
      token.typesEndingAtToken.map(_.name)
    }
  }
}
