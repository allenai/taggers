package edu.knowitall.taggers.tag;

import org.jdom2.Element;

public class ParseTagException extends Throwable {
    private static final long serialVersionUID = 1L;

    public ParseTagException(String message, Element e) {
        super(message + " for element '" + e.getName() + "'.");
    }

    public ParseTagException(String message, Exception e) {
        super(message, e);
    }

    public ParseTagException(Exception e) {
        super(e);
    }
}
