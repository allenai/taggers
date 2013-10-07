# Taggers

A tagger is a function from a sentence to a list of `Type`s.  A `Type`
consists of a name and the token interval it came from in the source
sentence.

## Example

For example, you might have a tagger that identifies animals.  Following
is the string serialized form of a tagger.  To the left of `:=` is the
name of the tagger--that is when the tagger finds a type it will have 
this name.  To the right of `:=` is the tagger class.  This is a Java/Scala class;
if no package is specified `taggers` will look in `edu.knowitall.taggers.tag`.
Between the braces `{}` are the arguments to the tagger.

```
Animal := NormalizedKeywordTagger {
  cat
  kitten
  dog
  puppy
}
```

If this tagger were to run over the following sentence, we would get some
types.

> Kittens are very cute , but they turn into cats .

```
Type(name=Animal, text="Kittens", interval=[0, 1))
Type(name=Animal, text="cats", interval=[10, 11))
```


## Running

The `taggers` project is composed of two subprojects: `tagger-core`, which 
contains the algorithms, and `tagger-webapp`, which contains the web demo.
The project is built with `sbt`.  For example, to run the web demo, you can
execute the following command.

```
sbt compile 'project tagger-webapp' run
```

If you want an example of how to use the taggers project as a dependency,
please look at `tagger-webapp`.

## PatternTagger

This tagger compiles regular expressions over the tokens in a sentence into an
NFA.  A token is described as a logical formula between angled brackets `<>`.
There are a number of fields that can be matched upon for each token.

* __string__
* __lemma__
* __pos__: the part-of-speech tag
* __chunk__: the chunk tag
* __type__: any type that intersects this token
* __typeStart__: any type that starts at this token
* __typeEnd__: any type that ends at this token
* __typeCont__: any type that intersects at this token but does not start or end there

A field can be matched in one of three ways.

1.  With double quotes `"`.  Strings are interpreted the same was as Java strings (backslash is the escape character).
2.  With single quotes `'`.  The text between two single quotes will be taken verbatim (there is no escape character).
3.  With slashes `/`.  The text between the slashes will be interpreted as a
    regular expression.  Backslash is the escape character so `\\` becomes a single
    backslash and `\/` escapes the forward slash.

A pattern tagger makes types with the tagger name, but also `LinkedType`s for each matching
group. A `LinkedType` has an Optional `Type` field that points to its parent `Type` and a name 
field with a special syntax. If the tagger is named `T` and a matching group is named `G1` for example, 
the tagger will create a `LinkedType` with the name `T.G1`.  If there is an unnamed matching group a `LinkedType`
will be created with the group number (i.e. `T.1`).

There is a lot of redundancy in their expressiveness. For example,
PatternTagger supports pattern matching on the fields .This is not necessary
but is an optimization and a shorthand.  For example, the following two'
patterns match the same text.

```
<pos=/NNPS?/> 
<pos="NNP"> | <pos="NNPS">
```

Here are some more equivalent examples:

```
<pos="JJ">* <pos=/NNP./>+
<pos="JJ">* <pos=/NNPS?/>+
<pos="JJ">* <pos="NNP" | pos="NNPS">+
<pos="JJ">* (?:<pos="NNP"> | <pos="NNPS">)+
```

Note that (3) and (4) are not preferred for efficiency reasons. Regex OR
(in example (4)) should only be used on multi-token sequences.

The Regular Expressions support named groups `(<name>: ... )`, unnamed
groups `(?: ... )`, and capturing groups `( ... )`. The operators allowed are
`+`, `?`, `*`, and `|`. The Logic Expressions (that describe each token) allow
grouping `( ... )`, not `!`, or `|`, and and `&`.  To learn more about
the regular expression language, see https://github.com/knowitall/openregex.

## TypePatternTagger

The `TypePatternTagger` extends the `PatternTagger` with a defintiion that matches
types.  Since a type can span multiple tokens but the pattern language
operates on the token level, matching types can be tedious and error prone.
For example, if you want to match the type `Animal`, you need the following pattern.

```
(?:(?:<typeStart='Animal' & typeEnd='Animal'>) | (?: <typeStart='Animal'> <typeCont='Animal'>* <typeEnd='Animal'>))
```

Matching many types quickly makes unreadable patterns, so the `TypePatternTagger` adds
the syntax `@Type` which, if the type is Animal (`@Animal`) it would expand into the above.
With this syntax, it's easy to match on types.  For an implementation of `ReVerb`, see
`examples/reverb.tc`.

## Extended Example

The following example demonstrates how to use the API for collecting and manipulating Type
objects with a specified TaggerCollection and test input.
```
import edu.knowitall.taggers.TaggerCollection
import edu.knowitall.taggers.LinkedType

object Example {
  val pattern = """
    Animal := NormalizedKeywordTagger {
      cat
      kitten
      dog
      puppy
    }
    Color := NormalizedKeywordTagger{
      blue
      red
      yellow
      green
    }
    ColorfulAnimal := PatternTagger {
      //namedGroup color will yield a Type object
      //that is linked to the ColorfulAnimal Type object
      (<color>:<type='Color'>) <type='Animal'>
    }
    ColorfulAnimalAction := TypePatternTagger{
      //TypePatternTagger supports @ syntax to capture
      // the entire Type
      @ColorfulAnimal <pos='VBD'>
    }
    """
    
  val input = """
    I have a red dog.
    Cliff has a yellow puppy.
    The yellow puppy ran.
    """
  def main(args: Array[String]){

    //instantiate TaggerCollection with the pattern input string
    val t = TaggerCollection.fromString(pattern)
    
    // collect lines from input string
    val lines = input.split("\n").map(f => f.trim()).filter(f => f!= "").toList
    

    for (line <- lines){
      //Run the patterns over the line and get resulting Type objects
      val types = t.tag(line).toList
      
      //output Type information
      println("Line: " + line)
      for(typ <- types){
        println("TaggerName: " +typ.name + "\tTypeInterval: " + typ.tokenInterval + "\t TypeText: " + typ.text)
      }
      
      //filter out the LinkedTypes
      for(typ <- types.filter(p => p.isInstanceOf[LinkedType])){
        val linkedTyp = typ.asInstanceOf[LinkedType]
        val linkedTypName = linkedTyp.name.split("\\.")(1)
        if(linkedTypName == "color"){
          println("COLOR:\t" + linkedTyp.text)
        }

        println()
      }
    }
  }
}
```

```
Line: I have a red dog.
TaggerName: Animal	TypeInterval: {4}	 TypeText: dog
TaggerName: Color	TypeInterval: {3}	 TypeText: red
TaggerName: ColorfulAnimal	TypeInterval: [3, 5)	 TypeText: red dog
TaggerName: ColorfulAnimal.color	TypeInterval: {3}	 TypeText: red
COLOR:	red

Line: Cliff has a yellow puppy.
TaggerName: Animal	TypeInterval: {4}	 TypeText: puppy
TaggerName: Color	TypeInterval: {3}	 TypeText: yellow
TaggerName: ColorfulAnimal	TypeInterval: [3, 5)	 TypeText: yellow puppy
TaggerName: ColorfulAnimal.color	TypeInterval: {3}	 TypeText: yellow
COLOR:	yellow

Line: The yellow puppy ran.
TaggerName: Animal	TypeInterval: {2}	 TypeText: puppy
TaggerName: Color	TypeInterval: {1}	 TypeText: yellow
TaggerName: ColorfulAnimal	TypeInterval: [1, 3)	 TypeText: yellow puppy
TaggerName: ColorfulAnimal.color	TypeInterval: {1}	 TypeText: yellow
TaggerName: ColorfulAnimalAction	TypeInterval: [1, 4)	 TypeText: yellow puppy ran
COLOR:	yellow

```
