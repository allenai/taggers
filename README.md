# Taggers

A tagger is a function from a sentence to a list of `Type`s.  A `Type`
consists of a name and the token interval it came from in the source
sentence.

## Example

For example, you might have a tagger that identifies animals.  Following is the
string serialized form of a tagger.  To the left of `:=` is the name of the
tagger - when the tagger finds a type it will have this name.  To the
right of `:=` is the tagger class.  This is a Java/Scala class; if no package
is specified `taggers` will look in `edu.knowitall.taggers.tag`.  Between the
braces `{}` are the arguments to the tagger.

```
Animal := LemmatizedKeywordTagger {
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

The `taggers` project is composed of three subprojects: `core`, which contains
the algorithms, `cli` which has a small cli application, and `webapp`, which
contains the web demo.  The project is built with `sbt`.  For example, to run
the web demo, you can execute the following command.

```
sbt compile 'project webapp' run
```

You can also run taggers as a cli.

```
sbt compile 'project cli' 'run examples/reverb.taggers'
```

If you want an example of how to use the taggers project as a dependency,
please look at `taggers-webapp`.

## PatternTagger

This tagger compiles regular expressions over the tokens in a sentence into an
NFA.  A token is described as a logical formula between angled brackets `<>`.
There are a number of fields that can be matched upon for each token.

* __string__
* __lemma__: the lemmatized form of the token string (see MorphaStemmer in nlptools)
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

If a quotation prefixed by "i" then the match will be case-insensitive (i.e.
`string = i"cat"` will match "cAt").

A pattern tagger makes types with the tagger name, but also `LinkedType`s for
each matching group. A `LinkedType` has an Optional `Type` field that points to
its parent `Type` and a name field with a special syntax. If the tagger is
named `T` and a matching group is named `G1` for example, the tagger will
create a `LinkedType` with the name `T.G1`.  If there is an unnamed matching
group a `LinkedType` will be created with the group number (i.e. `T.1`).

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

Named groups create output subtypes.  For example, if we had the following
`PatternTagger` applied to the example below.

```
DescribedNoun := PatternTagger {
    (<Description>:<pos='JJ'>+) (<Noun>:<pos='NN'>+)
}
```

> The huge fat cat lingered in the hallway.

We would get the following output types.

```
DescribedNoun(huge fat cat)
DescribedNoun.Description(huge fat)
DescribedNoun.Noun(cat)
```

## TypePatternTagger

The `TypePatternTagger` extends the `PatternTagger` with added syntax to match
types.  Since a type can span multiple tokens but the pattern language operates
on the token level, matching types can be tedious and error prone.  For
example, if you want to match the type `Animal`, you need the following
pattern.

```
(?:(?:<typeStart='Animal' & typeEnd='Animal'>) | (?: <typeStart='Animal' & !typeEnd='Animal'> <typeCont='Animal'>* <typeEnd='Animal'>))
```

Matching many types in this manner quickly makes unreadable patterns, so the
`TypePatternTagger` adds the syntax `@Type` which, if the type is Animal
(`@Animal`) it would expand into the above expression.  With this syntax, it's
easy to match on types.  For an implementation of `ReVerb`, see
`examples/reverb.taggers`.
