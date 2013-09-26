package edu.knowitall.taggers.tag

import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set
import java.util.TreeMap
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.jdom2.Element
import com.google.common.base.Predicate
import com.google.common.collect.ImmutableList
import edu.knowitall.taggers.Type
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.washington.cs.knowitall.logic.ArgFactory
import edu.washington.cs.knowitall.logic.LogicExpression
import edu.washington.cs.knowitall.regex.Expression.BaseExpression
import edu.washington.cs.knowitall.regex.Expression.NamedGroup
import edu.washington.cs.knowitall.regex.ExpressionFactory
import edu.washington.cs.knowitall.regex.Match
import edu.washington.cs.knowitall.regex.RegularExpression
import edu.knowitall.openregex
import edu.knowitall.taggers.pattern.PatternBuilder
import edu.knowitall.taggers.pattern.TypedToken
import scala.collection.JavaConverters._

/**
 * *
 * Run a token-based pattern over the text and tag matches.
 *
 * @author schmmd
 *
 */
class PatternTagger(descriptor: String, expressions: Seq[String]) extends Tagger(descriptor, null) {
  val patterns: Seq[openregex.Pattern[PatternBuilder.Token]] = this.compile(expressions)

  // for reflection
  def this(descriptor: String, expressions: java.util.List[String]) = {
    this(descriptor, expressions.asScala.toSeq)
  }

  protected def this(descriptor: String) {
    this(descriptor, null: Seq[String])
  }

  override def sort() = {}

  private def compile(expressions: Seq[String]) = {
    expressions map PatternBuilder.compile
  }

  override def findTags(sentence: java.util.List[Lemmatized[ChunkedToken]]) = {
    this.findTagsWithTypes(sentence, Collections.emptyList[Type])
  }

  /**
   * This method overrides Tagger's default implementation. This
   * implementation uses information from the Types that have been assigned to
   * the sentence so far.
   */
  override def findTagsWithTypes(sentence: java.util.List[Lemmatized[ChunkedToken]],
    originalTags: java.util.List[Type]): java.util.List[Type] = {

    // create a java set of the original tags
    val originalTagSet = originalTags.asScala.toSet

    // convert tokens to TypedTokens
    val typedTokens = for ((token, i) <- sentence.asScala.zipWithIndex) yield {
      new TypedToken(token, i, originalTagSet.filter(_.interval contains i))
    }

    val tags = for {
      pattern <- patterns
      tag <- this.findTags(typedTokens, sentence, pattern)
    } yield (tag)

    return tags.asJava;
  }

  /**
   * This is a helper method that creates the Type objects from a given
   * pattern and a List of TypedTokens.
   *
   * Matching groups will create a type with the name or index
   * appended to the descriptor.
   *
   * @param typedTokenSentence
   * @param sentence
   * @param pattern
   * @return
   */
  protected def findTags(typedTokenSentence: Seq[TypedToken],
    sentence: List[Lemmatized[ChunkedToken]],
    pattern: openregex.Pattern[TypedToken]) = {

    var tags = Seq.empty[Type]

    val matches = pattern.findAll(typedTokenSentence);
    for (m <- matches) {
      val groupSize = m.groups.size
      for (i <- 0 until groupSize) {
        val group = m.groups(i);

        val postfix = "";
        group.expr match {
          case _ if i == 0 => ""
          case namedGroup: NamedGroup[_] => "." + namedGroup.name
          case _ => "." + i
        }
        val tag = Type.fromSentence(sentence, this.descriptor + postfix,
          this.source, group.interval);
        tags = tags :+ tag
      }
    }

    tags
  }

  // / XML
  /*
    public PatternTagger(Element e) throws ParseTagException,
            SecurityException, IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException {
        super(e);

        Map<String, String> variables = new TreeMap<String, String>();
        for (Element variable : e.getChildren("variable")) {
            String name = variable.getAttributeValue("name");
            String value = variable.getText().trim();

            variables.put(name, value);
        }

        Element patterns = e.getChild("patterns");
        if (patterns == null) {
            throw new ParseTagException("No element 'patterns'", e);
        }

        List<String> expressions = new ArrayList<String>(patterns.getChildren()
                .size());
        for (Element pattern : patterns.getChildren("pattern")) {
            String expression = pattern.getText().trim();

            // perform variable substitutions
            for (String name : variables.keySet()) {
                expression = expression.replaceAll(
                        Pattern.quote("${" + name + "}"), variables.get(name));
            }

            expressions.add(expression);
        }

        this.expressions = ImmutableList.copyOf(expressions);
        this.patterns = this.compile(this.expressions);
    }

    @Override
    public Element toXmlElement() {
        Element e = super.toXmlElement();

        Element patterns = new Element("patterns");
        for (String pattern : this.expressions) {
            patterns.addContent(new Element("pattern").setText(pattern));
        }

        e.addContent(patterns);

        return e;
    }

    public static Interval intervalFromGroup(Match.Group<?> group) {
        int startIndex = group.startIndex();
        int endIndex = group.endIndex() + 1;

        if (startIndex == -1 || endIndex == -1) {
            return Interval$.MODULE$.empty();
        } else {
            return Interval$.MODULE$.open(startIndex, endIndex);
        }
    }
    */

  /**
   * *
   * This class compiles regular expressions over the tokens in a sentence
   * into an NFA. There is a lot of redundancy in their expressiveness. This
   * is largely because it supports pattern matching on the fields This is not
   * necessary but is an optimization and a shorthand (i.e.
   * {@code <pos="NNPS?"> is equivalent to "<pos="NNP" | pos="NNPS">} and
   * {@code (?:<pos="NNP"> | <pos="NNPS">)}.
   * <p>
   * Here are some equivalent examples:
   * <ol>
   * <li> {@code <pos="JJ">* <pos="NNP.">+}
   * <li> {@code <pos="JJ">* <pos="NNPS?">+}
   * <li> {@code <pos="JJ">* <pos="NNP" | pos="NNPS">+}
   * <li> {@code <pos="JJ">* (?:<pos="NNP"> | <pos="NNPS">)+}
   * </ol>
   * Note that (3) and (4) are not preferred for efficiency reasons. Regex OR
   * (in example (4)) should only be used on multi-token sequences.
   * <p>
   * The Regular Expressions support named groups (<name>: ... ), unnamed
   * groups (?: ... ), and capturing groups ( ... ). The operators allowed are
   * +, ?, *, and |. The Logic Expressions (that describe each token) allow
   * grouping "( ... )", not '!', or '|', and and '&'.
   *
   * @param regex
   * @return
   */
  /*
    public static RegularExpression<TypedToken> makeRegex(String regex) {
        return RegularExpression.compile(regex,
                new ExpressionFactory<TypedToken>() {

                    @Override
                    public BaseExpression<TypedToken> create(
                            final String expression) {
                        final Pattern valuePattern = Pattern
                                .compile("([\"'])(.*)\\1");
                        return new BaseExpression<TypedToken>(expression) {
                            private final LogicExpression<TypedToken> logic;

                            {
                                this.logic = LogicExpression.compile(
                                        expression,
                                        new ArgFactory<TypedToken>() {
                                            @Override
                                            public edu.washington.cs.knowitall.logic.Expression.Arg<TypedToken> create(
                                                    final String argument) {
                                                return new edu.washington.cs.knowitall.logic.Expression.Arg<TypedToken>() {
                                                    private final Expression expression;

                                                    {
                                                        String[] parts = argument
                                                                .split("=");

                                                        String base = parts[0];

                                                        Matcher matcher = valuePattern
                                                                .matcher(parts[1]);
                                                        if (!matcher.matches()) {
                                                            throw new IllegalArgumentException(
                                                                    "Value not enclosed in quotes (\") or ('): "
                                                                            + argument);
                                                        }
                                                        String string = matcher
                                                                .group(2);

                                                        if (base.equalsIgnoreCase("stringCS")) {
                                                            this.expression = new StringExpression(
                                                                    string, 0);
                                                        } else if (base
                                                                .equalsIgnoreCase("string")) {
                                                            this.expression = new StringExpression(
                                                                    string);
                                                        } else if (base
                                                                .equalsIgnoreCase("lemma")) {
                                                            this.expression = new LemmaExpression(
                                                                    string);
                                                        } else if (base
                                                                .equalsIgnoreCase("pos")) {
                                                            this.expression = new PosTagExpression(
                                                                    string);
                                                        } else if (base
                                                                .equalsIgnoreCase("chunk")) {
                                                            this.expression = new ChunkTagExpression(
                                                                    string);
                                                        } else if (base
                                                                .equalsIgnoreCase("type")
                                                                || base.equalsIgnoreCase("typeStart")
                                                                || base.equalsIgnoreCase("typeEnd")) {
                                                            this.expression = new TypeTagExpression(
                                                                    string,
                                                                    base);
                                                        } else {
                                                            throw new IllegalStateException(
                                                                    "unknown argument specified: "
                                                                            + base);
                                                        }
                                                    }

                                                    @Override
                                                    public boolean apply(
                                                            TypedToken entity) {
                                                        return this.expression
                                                                .apply(entity);
                                                    }
                                                };
                                            }
                                        });
                            }

                            @Override
                            public boolean apply(TypedToken entity) {
                                return logic.apply(entity);
                            }
                        };
                    }
                });
    }

    /***
     * An expression that is evaluated against a TypedToken.
     *
     * @author schmmd
     *
     */
    protected static abstract class Expression implements Predicate<TypedToken> {
    }

    /***
     * A regular expression that is evaluated against the string portion of a
     * TypedToken.
     *
     * @author schmmd
     *
     */
    protected static class StringExpression extends Expression {
        final Pattern pattern;

        public StringExpression(String string, int flags) {
            pattern = Pattern.compile(string, flags);
        }

        public StringExpression(String string) {
            this(string, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean apply(TypedToken token) {
            return pattern.matcher(token.token().token().string()).matches();
        }
    }

    /***
     * A regular expression that is evaluated against the lemma portion of a
     * TypedToken.
     *
     * @author schmmd
     *
     */
    protected static class LemmaExpression extends Expression {
        final Pattern pattern;

        public LemmaExpression(String string, int flags) {
            pattern = Pattern.compile(string, flags);
        }

        public LemmaExpression(String string) {
            this(string, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean apply(TypedToken token) {
            return pattern.matcher(token.token().lemma()).matches();
        }
    }

    /***
     * A regular expression that is evaluated against the POS tag portion of a
     * TypedToken.
     *
     * @author schmmd
     *
     */
    protected static class PosTagExpression extends Expression {
        final Pattern pattern;

        public PosTagExpression(String string, int flags) {
            pattern = Pattern.compile(string, flags);
        }

        public PosTagExpression(String string) {
            this(string, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean apply(TypedToken token) {
            return pattern.matcher(token.token().token().postag()).matches();
        }
    }

    /***
     * A regular expression that is evaluated against the chunk tag portion of a
     * TypedToken.
     *
     * @author schmmd
     *
     */
    protected static class ChunkTagExpression extends Expression {
        final Pattern pattern;

        public ChunkTagExpression(String string, int flags) {
            pattern = Pattern.compile(string, flags);
        }

        public ChunkTagExpression(String string) {
            this(string, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean apply(TypedToken token) {
            return pattern.matcher(token.token().token().chunk()).matches();
        }
    }

    /**
     * A regular expression that is evaluated agains the type tag of a
     * TypedToken.
     *
     * @author jgilme1
     *
     */
    protected static class TypeTagExpression extends Expression {
        final Pattern pattern;
        final String typeMatchType;

        public TypeTagExpression(String string, int flags, String base) {
            pattern = Pattern.compile(string, flags);
            typeMatchType = base;
        }

        public TypeTagExpression(String string, String base) {
            this(string, Pattern.CASE_INSENSITIVE, base);
        }

        @Override
        public boolean apply(TypedToken token) {

            Iterable<Type> types = null;
            if (typeMatchType.equals("type")) {
                types = token.types();
            } else if (typeMatchType.equals("typeStart")) {
                types = token.typesBeginningAtToken();

            } else if (typeMatchType.equals("typeEnd")) {
                types = token.typesEndingAtToken();
            } else {
                types = new ArrayList<Type>();
            }

            for (Type t : types) {
                if (pattern.matcher(t.descriptor()).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class TypedToken {

        private Lemmatized<ChunkedToken> token;
        private Set<Type> types = new HashSet<Type>();
        private Set<Type> typesBeginningAtToken = new HashSet<Type>();
        private Set<Type> typesEndingAtToken = new HashSet<Type>();

        /***
         * The constructor creates three different Type Sets associated with the
         * input Token. types is a Type Set that stores all of the intersecting
         * types at a given Token. typesBeginningAtToken is a Type Set that
         * stores all of the types with the same ending offset as the Token
         * typesEndingAtToken is a Type Set that stores all of the types with
         * the same beginning offset as the Token
         *
         * @param tokenIndex
         * @param types
         * @param sentence
         */
        public TypedToken(Integer tokenIndex, Set<Type> types,
                List<Lemmatized<ChunkedToken>> sentence) {
            token = sentence.get(tokenIndex);
            Interval tokenInterval = Interval.closed(tokenIndex, tokenIndex);
            for (Type t : types) {
                if (t.interval().intersects(tokenInterval)) {
                    this.types.add(t);
                    if (t.interval().start() == tokenInterval.start()) {
                        this.typesBeginningAtToken.add(t);
                    }
                    if (t.interval().end() == tokenInterval.end()) {
                        this.typesEndingAtToken.add(t);
                    }
                }
            }
        }

        public Lemmatized<ChunkedToken> token() {
            return this.token;
        }

        public Set<Type> types() {
            return this.types;
        }

        public Set<Type> typesBeginningAtToken() {
            return this.typesBeginningAtToken;
        }

        public Set<Type> typesEndingAtToken() {
            return this.typesEndingAtToken;
        }

    }
    */
}