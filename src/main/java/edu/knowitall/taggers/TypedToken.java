package edu.knowitall.taggers;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

import edu.knowitall.taggers.Type;
import edu.knowitall.tool.stem.Lemmatized;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.collection.immutable.Interval;


public class TypedToken {
  
  private Lemmatized<ChunkedToken> token;
  private Set<Type> types = new HashSet<Type>();
  private Set<Type> typesBeginningAtToken = new HashSet<Type>();
  private Set<Type> typesEndingAtToken = new HashSet<Type>();
  
  /***
   * The constructor creates three different Type Sets associated with the
   * input Token.
   * types is a Type Set that stores all of the intersecting types at a given Token.
   * typesBeginningAtToken is a Type Set that stores all of the types with the same
   * 	ending offset as the Token
   * typesEndingAtToken is a Type Set that stores all of the types with the same
   * 	beginning offset as the Token
   * @param tokenIndex
   * @param types
   * @param sentence
   */
  public TypedToken(Integer tokenIndex, Set<Type> types, List<Lemmatized<ChunkedToken>> sentence){
	  token = sentence.get(tokenIndex);
	  Interval tokenInterval = Interval.closed(tokenIndex, tokenIndex);
	  for(Type t : types){
		  if(t.interval().intersects(tokenInterval)){
			 this.types.add(t); 
			 if(t.interval().start() == tokenInterval.start()){
				 this.typesBeginningAtToken.add(t);
			 }
			 if(t.interval().end() == tokenInterval.end()){
				 this.typesEndingAtToken.add(t);
			 }
		  }
	  }
  }
  
  public Lemmatized<ChunkedToken> token(){
	  return this.token;
  }
  
  public Set<Type> types(){
	  return this.types;
  }
  
  public Set<Type> typesBeginningAtToken(){
	  return this.typesBeginningAtToken;
  }
  
  public Set<Type> typesEndingAtToken(){
	  return this.typesEndingAtToken;
  }

}