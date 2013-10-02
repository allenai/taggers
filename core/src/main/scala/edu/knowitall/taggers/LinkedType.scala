package edu.knowitall.taggers

import edu.knowitall.tool.typer.Type

case class LinkedType(typ: Type, link: Option[Type] = None) extends Type {
  override def name = typ.name
  override def source = typ.source
  override def tokenInterval = typ.tokenInterval
  override def text = typ.text
}
