package com.aljodomo.kontis.utils;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
public class StringUtils {
    public static String concat(List<String> words) {
        StringBuilder builder = new StringBuilder();
        words.forEach(s -> builder.append(s).append(" "));
        return builder.toString().trim();
    }
}
