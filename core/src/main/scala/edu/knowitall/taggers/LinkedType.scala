package edu.knowitall.taggers

import edu.knowitall.tool.typer.Type

class LinkedType(val typ: Type, val link: Option[Type] = None) extends Type {
  override def name = typ.name
  override def source = typ.source
  override def tokenInterval = typ.tokenInterval
  override def text = typ.text
}
class NamedGroupType(val groupName: String, typ: Type, link: Option[Type] = None) extends LinkedType(typ,link){
}
