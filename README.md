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

A pattern tagger makes types with the tagger name, but also types for each matching
group.  If the tagger is named `T` and a matching group is named `G1` for example, the
tagger will create `T.G1`.  If there is an unnamed matching group a type will be created
with the group number (i.e. `T.1`).

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
