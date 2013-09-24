package edu.knowitall.taggers

import org.scalatest.FlatSpec
import scala.io.Source
import edu.knowitall.taggers.tag.TaggerCollection
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.collection.immutable.Interval

class PatternTaggerSpec extends FlatSpec{
  
  "WorldCandy PatternTagger" should "match an occurrence from a sequence of Type Nationality and Type Candy" in{
    //load keyword taggers candies.xml, and nationalities.xml from resources
    //load PatternTagger worldcandies.xml from resources
    val taggerCollection = TaggerCollection.fromPath(getClass.getResource("/edu/knowitall/taggers").getPath())
    
    //test sentence that should be tagged as
    // WorldCandy{(3,5)}
    val testSentence = "Vernon enjoys eating Argentinian Licorice."
    
    val chunker = new OpenNlpChunker();
    val morpha = new MorphaStemmer();
   
    
    var tokens = List[Lemmatized[ChunkedToken]]()
    val chunkedSentence = chunker.chunk(testSentence)
    
    for(ct <- chunkedSentence){
      tokens =  tokens :+ morpha.lemmatizeToken(ct)
    }
    
    //Tag the sentence with the loaded taggers
    val types = scala.collection.JavaConversions.collectionAsScalaIterable(taggerCollection.tag(scala.collection.JavaConversions.seqAsJavaList(tokens)))
    
    //matching interval should be [3,5)
    val worldCandyInterval = Interval.open(3,5)
    

    //Option that is undefined if no type matches
    //the worldCandyInterval or it is defined as 
    //that Type
    var targetTypeOption :Option[Type] = None
    
    //iterate over all the types returned
    //searching for a type that matches
    //the worldCandyInterval
    for(t <- types){
      if(t.interval().equals(worldCandyInterval)){
        targetTypeOption = Some(t)
      }
    }
    
    //assert that the worldCandyInterval has been matched
    //and that it has been tagged as a ''World Candy''
    assert(targetTypeOption.isDefined)
    assert(targetTypeOption.get.descriptor() == "WorldCandy")
  }
  

}