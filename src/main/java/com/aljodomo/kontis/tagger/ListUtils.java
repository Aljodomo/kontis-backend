package com.aljodomo.kontis.tagger;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
public class ListUtils {
    public static String concat(List<String> words) {
        StringBuilder builder = new StringBuilder();
        words.forEach(s -> builder.append(s).append(" "));
        return builder.toString().trim();
    }
}
