package edu.knowitall.taggers;

import com.google.common.base.Function;

public class StringFunctions {
    public static final Function<String, String> toLowerCase = new Function<String, String>() {
        @Override
        public String apply(String string) {
            return string.toLowerCase();
        }
    };

    public static Function<String, String[]> split(final String regex) {
        return new Function<String, String[]>() {
            @Override
            public String[] apply(String string) {
                return string.split(regex);
            }
        };
    }
}
