package edu.knowitall.taggers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.knowitall.tool.chunk.ChunkedToken;
import edu.knowitall.tool.stem.Lemmatized;
import edu.knowitall.tool.tokenize.Token;

public class SentenceFunctions {
  public static List<Lemmatized<ChunkedToken>> lowercase(List<Lemmatized<ChunkedToken>> tokens) {
    List<Lemmatized<ChunkedToken>> list = new ArrayList<Lemmatized<ChunkedToken>>(tokens.size());

    for (Lemmatized<ChunkedToken> token : tokens) {
        list.add(new Lemmatized<ChunkedToken>(new ChunkedToken(token.token().chunk(), token.token().postag(), token.token().string().toLowerCase(), token.token().offset()), token.lemma()));
    }

    return list;
  }

  public static List<String> strings(List<Lemmatized<ChunkedToken>> tokens) {
    List<String> list = new ArrayList<String>(tokens.size());

    for (Lemmatized<ChunkedToken> token : tokens) {
        list.add(token.token().string());
    }

    return list;
  }

  public static List<Token> tokens(List<Lemmatized<ChunkedToken>> tokens) {
    List<Token> list = new ArrayList<Token>(tokens.size());

    for (Lemmatized<ChunkedToken> token : tokens) {
        list.add(new Token(token.token().string(), token.token().offset()));
    }

    return list;
  }

  public static List<String> lemmas(List<Lemmatized<ChunkedToken>> tokens) {
    List<String> list = new ArrayList<String>(tokens.size());

    for (Lemmatized<ChunkedToken> token : tokens) {
        list.add(token.lemma());
    }

    return list;
  }

  public static String text(List<Lemmatized<ChunkedToken>> tokens) {
    return StringUtils.join(strings(tokens), " ");
  }
}
