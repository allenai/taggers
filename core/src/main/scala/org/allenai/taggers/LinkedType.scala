package org.allenai.taggers

import org.allenai.nlpstack.core.typer.Type

class LinkedType(val typ: Type, val link: Option[Type] = None) extends Type {
  override def name = typ.name
  override def source = typ.source
  override def tokenInterval = typ.tokenInterval
  override def text = typ.text

  override def toString() = "LinkedType(" + name + "," + source + "," + tokenInterval + "," + text + ")"

  override def equals(that: Any) = that match {
    case that: LinkedType => that.canEqual(this) && that.hashCode() == this.hashCode()
    case _ => false
  }
  def canEqual(that: Any) = that.isInstanceOf[LinkedType]
  override def hashCode = (41 *
    (41 *
      (41 * name.hashCode) + tokenInterval.hashCode) + text.hashCode) + link.hashCode
}

class NamedGroupType(val groupName: String, typ: Type, link: Option[Type] = None) extends LinkedType(typ, link) {
  override def toString() = "NamedGroupType(" + groupName + "," + name + "," + source + "," + tokenInterval + "," + text + ")"
  override def canEqual(that: Any) = that.isInstanceOf[NamedGroupType]
  override def hashCode = (41 *
    (41 *
      (41 *
        (41 * groupName.hashCode) + name.hashCode) + tokenInterval.hashCode) + text.hashCode
    ) + link.hashCode
  override def equals(that: Any) = that match {
    case that: NamedGroupType => that.canEqual(this) && that.hashCode() == this.hashCode()
    case _ => false
  }
}
