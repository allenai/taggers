package edu.knowitall.taggers

import edu.knowitall.tool.typer.Type
import edu.knowitall.common.HashCodeHelper

class LinkedType(val typ: Type, val link: Option[Type] = None) extends Type {
  override def name = typ.name
  override def source = typ.source
  override def tokenInterval = typ.tokenInterval
  override def text = typ.text
  
  override def toString() = "LinkedType(" + name +","+source+","+tokenInterval+","+text+")"

  override def equals(that:Any) = that match{
    case that: LinkedType =>  that.canEqual(this) && that.hashCode() == this.hashCode()
    case _ => false
  }
  def canEqual(that: Any) = that.isInstanceOf[LinkedType]
  override def hashCode = HashCodeHelper(name,tokenInterval,text,link)
  
}
class NamedGroupType(val groupName: String, typ: Type, link: Option[Type] = None) extends LinkedType(typ,link){
  override def toString() = "NamedGroupType(" + groupName + "," +  name +","+source+","+tokenInterval+","+text+")"
  override def canEqual(that: Any) = that.isInstanceOf[NamedGroupType]
  override def hashCode = HashCodeHelper(groupName,this.name,this.tokenInterval,this.text,this.link)
  override def equals(that:Any) = that match{
    case that: NamedGroupType => that.canEqual(this) && that.hashCode() == this.hashCode()
    case _ => false
  }
}