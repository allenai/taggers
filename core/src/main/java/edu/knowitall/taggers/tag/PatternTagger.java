package edu.knowitall.taggers.tag;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.jdom2.Element;

import scala.collection.JavaConverters;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.collection.immutable.Interval$;
import edu.knowitall.taggers.ListUtils;
import edu.knowitall.taggers.Type;
import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;
import edu.washington.cs.knowitall.logic.ArgFactory;
import edu.washington.cs.knowitall.logic.LogicExpression;
import edu.washington.cs.knowitall.regex.Expression.BaseExpression;
import edu.washington.cs.knowitall.regex.ExpressionFactory;
import edu.washington.cs.knowitall.regex.Match;
import edu.washington.cs.knowitall.regex.RegularExpression;

/***
 * Run a token-based pattern over the text and tag matches.
 * @author schmmd
 *
 */
public class PatternTagger extends Tagger {
    public ImmutableList<RegularExpression<TypedToken>> patterns;
    public ImmutableList<String> expressions;

    protected PatternTagger(String descriptor) {
        super(descriptor, null);
        patterns = null;
        expressions = null;
    }

    public PatternTagger(String descriptor, List<String> expressions) {
        super(descriptor, null);
        this.expressions = ImmutableList.copyOf(expressions);
        this.patterns = this.compile(this.expressions);
    }

    public PatternTagger(String descriptor, String pattern) {
        this(descriptor, Collections.singletonList(pattern));
        this.patterns = this.compile(this.expressions);
    }

    public void sort() {
    }

    private ImmutableList<RegularExpression<TypedToken>> compile(List<String> expressions) {
        List<RegularExpression<TypedToken>> patterns = new ArrayList<RegularExpression<TypedToken>>();
        for (String expression : expressions) {
            RegularExpression<TypedToken> pattern = PatternTagger.makeRegex(expression);
            patterns.add(pattern);
        }

        return ImmutableList.copyOf(patterns);
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) return false;
        if (this == that) return true;
        if (this.getClass() != that.getClass()) return false;

        PatternTagger kt = (PatternTagger)that;
        if (!super.equals(that)) {
            return false;
        }

        if (this.patterns.size() != kt.patterns.size()) {
            return false;
        }

        if (this.patterns.size() != kt.patterns.size()) {
            return false;
        }

        Iterator<RegularExpression<TypedToken>> i1 = this.patterns.iterator();
        Iterator<RegularExpression<TypedToken>> i2 = kt.patterns.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            if (!i1.equals(i2)) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Takes the list of Types found by the Tagger and nulls
     * the ones that are included in larger tags.
     * @param tags
     */
    private void nullTagsThatAreContainedByOtherTags(List<Type> tags){
    	
        // null tags contained by another tag
        for (int i = 0 ; i < tags.size(); i++) {
            Type tag = tags.get(i);

            for (Type compare : tags) {
                if (compare != null && tag != compare) {
                    if (compare.interval().superset(tag.interval())) {
                        tags.set(i, null);
                    }
                }
            }
        }
    }

    /**
     * Implementation of Tagger's abstract method findTags. This method
     * uses only information from the Lemmatized ChunkedTokens in the sentence
     * to match regular expressions. This method should not be used for the 
     * PatternTagger but it is implemented anyways.
     */
    @Override
    public List<Type> findTags(final List<Lemmatized<ChunkedToken>> sentence) {
        ArrayList<Type> tags = new ArrayList<Type>();
        
        //convert sentence to TypedToken sentence
        List<TypedToken> typedTokenSentence = new ArrayList<TypedToken>();
        for(int i =0; i < sentence.size(); i ++){
        	typedTokenSentence.add(new TypedToken(i, new HashSet<Type>(), sentence));
        }
        //Get all the pattern types from the chunkedTokens
        for (RegularExpression<TypedToken> pattern : patterns) {
            tags.addAll(this.findTags(typedTokenSentence, sentence, pattern));
        }
        
        // remove nulled tags
        nullTagsThatAreContainedByOtherTags(tags);
        ListUtils.removeNulls(tags);

        return tags;
    }
    
    /**
     * This method overrides Tagger's default implementation. This implementation uses
     * information from the Types that have been assigned to the sentence so far.
     */
    @Override 
    public List<Type> getTags(final List<Lemmatized<ChunkedToken>> sentence, final List<Type> previousTags){
    	ArrayList<Type> tags = new ArrayList<Type>();
    	Set<Type> tagSet = new HashSet<Type>();
    	for(Type t: previousTags){
    		tagSet.add(t);
    	}
        //convert sentence to TypedToken sentence
        List<TypedToken> typedTokenSentence = new ArrayList<TypedToken>();
        for(int i =0; i < sentence.size(); i ++){
        	typedTokenSentence.add(new TypedToken(i, tagSet, sentence));
        }
        
        
        for (RegularExpression<TypedToken> pattern : patterns) {
            tags.addAll(this.findTags(typedTokenSentence, sentence, pattern));
        }
        
        nullTagsThatAreContainedByOtherTags(tags);
        ListUtils.removeNulls(tags);
        return tags;	
    }

    /**
     * This is a helper method that creates the Type objects
     * from a given pattern and a List of TypedTokens.
     * @param typedTokenSentence
     * @param sentence
     * @param pattern
     * @return
     */
    protected List<Type> findTags(final List<TypedToken> typedTokenSentence,
            final List<Lemmatized<ChunkedToken>> sentence,
            final RegularExpression<TypedToken> pattern) {

        List<Type> tags = new ArrayList<Type>();

        List<Match<TypedToken>> matches = pattern.findAll(typedTokenSentence);
        for (Match<TypedToken> match : matches) {
            final Match.Group<TypedToken> group;

            int groupSize = match.groups().size();
            if (groupSize == 1) {
                group = match.groups().get(0);
            }
            else if (groupSize == 2) {
                group = match.groups().get(1);
            }
            else {
                throw new IllegalArgumentException("There must not be exactly more than one capture group.");
            }

            Type tag = Type.fromSentence(sentence, this.descriptor, this.source, intervalFromGroup(group));
            tags.add(tag);
        }

        return tags;
    }



    /// XML
    @SuppressWarnings("unchecked")
    public PatternTagger(Element e) throws ParseTagException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        super(e);

        Map<String, String> variables = new TreeMap<String, String>();
        for (Element variable : (List<Element>)e.getChildren("variable")) {
            String name = variable.getAttributeValue("name");
            String value = variable.getText().trim();

            variables.put(name, value);
        }

        Element patterns = e.getChild("patterns");
        if (patterns == null) {
            throw new ParseTagException("No element 'patterns'", e);
        }

        List<String> expressions = new ArrayList<String>(patterns.getChildren().size());
        for (Element pattern : (List<Element>)patterns.getChildren("pattern")) {
            String expression = pattern.getText().trim();

            // perform variable substitutions
            for (String name : variables.keySet()) {
                expression = expression.replaceAll(Pattern.quote("${" + name +"}"), variables.get(name));
            }

            expressions.add(expression);
        }

        this.expressions = ImmutableList.copyOf(expressions);
        this.patterns = this.compile(this.expressions);
    }

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
        }
        else {
            return Interval$.MODULE$.open(startIndex, endIndex);
        }
    }


    /***
     * This class compiles regular expressions over the tokens in a sentence
     * into an NFA.  There is a lot of redundancy in their expressiveness.
     * This is largely because it supports pattern matching on the fields
     * This is not necessary but is an optimization and a shorthand (i.e.
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
     * Note that (3) and (4) are not preferred for efficiency reasons.  Regex OR
     * (in example (4)) should only be used on multi-token sequences.
     * <p>
     * The Regular Expressions support named groups (<name>: ... ), unnamed
     * groups (?: ... ), and capturing groups ( ... ).  The operators allowed
     * are +, ?, *, and |.  The Logic Expressions (that describe each token)
     * allow grouping "( ... )", not '!', or '|', and and '&'.
     * @param regex
     * @return
     */
    public static RegularExpression<TypedToken> makeRegex(String regex) {
        return RegularExpression.compile(regex, new ExpressionFactory<TypedToken>() {

            @Override
            public BaseExpression<TypedToken> create(final String expression) {
                final Pattern valuePattern = Pattern.compile("([\"'])(.*)\\1");
                    return new BaseExpression<TypedToken>(expression) {
                        private final LogicExpression<TypedToken> logic;

                        {
                            this.logic = LogicExpression.compile(expression, new ArgFactory<TypedToken>() {
                                @Override
                                public edu.washington.cs.knowitall.logic.Expression.Arg<TypedToken> create(final String argument) {
                                    return new edu.washington.cs.knowitall.logic.Expression.Arg<TypedToken>() {
                                        private final Expression expression;

                                        {
                                            String[] parts = argument.split("=");

                                            String base = parts[0];

                                            Matcher matcher = valuePattern.matcher(parts[1]);
                                            if (!matcher.matches()) {
                                                throw new IllegalArgumentException("Value not enclosed in quotes (\") or ('): " + argument);
                                            }
                                            String string = matcher.group(2);

                                            if (base.equalsIgnoreCase("stringCS")) {
                                                this.expression = new StringExpression(string, 0);
                                            }
                                            else if (base.equalsIgnoreCase("string")) {
                                                this.expression = new StringExpression(string);
                                            }
                                            else if (base.equalsIgnoreCase("lemma")) {
                                                this.expression = new LemmaExpression(string);
                                            }
                                            else if (base.equalsIgnoreCase("pos")) {
                                                this.expression = new PosTagExpression(string);
                                            }
                                            else if (base.equalsIgnoreCase("chunk")) {
                                                this.expression = new ChunkTagExpression(string);
                                            }
                                            else if (base.equalsIgnoreCase("type")||
                                            		base.equalsIgnoreCase("typeStart")||
                                            		base.equalsIgnoreCase("typeEnd")){
                                            	this.expression = new TypeTagExpression(string,base);
                                            }
                                            else {
                                                throw new IllegalStateException("unknown argument specified: " + base);
                                            }
                                        }

                                        @Override
                                        public boolean apply(TypedToken entity) {
                                            return this.expression.apply(entity);
                                        }};
                                }});
                        }

                        @Override
                        public boolean apply(TypedToken entity) {
                            return logic.apply(entity);
                        }};
            }});
    }

    /***
     * An expression that is evaluated against a TypedToken.
     * @author schmmd
     *
     */
    protected static abstract class Expression implements Predicate<TypedToken> {
    }
    


    /***
     * A regular expression that is evaluated against the string portion of a
     * TypedToken.
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
     * @author jgilme1
     *
     */
    protected static class TypeTagExpression extends Expression{
    	final Pattern pattern;
    	final String typeMatchType;
    	
    	public TypeTagExpression(String string, int flags, String base){
    		pattern = Pattern.compile(string,flags);
    		typeMatchType = base;
    	}
    	
    	public TypeTagExpression(String string, String base){
    		this(string, Pattern.CASE_INSENSITIVE, base);
    	}
    	
        @Override
        public boolean apply(TypedToken token) {
        	
        	Iterable<Type> types = null;
        	if(typeMatchType.equals("type")){
        		types = token.types();
        	}
        	else if(typeMatchType.equals("typeStart")){
        		types = token.typesBeginningAtToken();

        	}
        	else if(typeMatchType.equals("typeEnd")){
        		types = token.typesEndingAtToken();
        	}
        	else{
        		types = new ArrayList<Type>();
        	}

        	for(Type t : types){
        		if(pattern.matcher(t.descriptor()).matches()){
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
}
