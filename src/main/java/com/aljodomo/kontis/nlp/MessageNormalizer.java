package com.aljodomo.kontis.nlp;

import org.springframework.stereotype.Component;

/**
 * @author Aljoscha Domonell
 */
@Component
public class MessageNormalizer {

    /**
     * Normalize a message in a reproducible way.
     */
    public String normalize(String message) {
        return message
                .toLowerCase()
                .replace("ß", "ss")
                .replace("ü", "ue")
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("/", " ")
                .replaceAll("[-+]", " ")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
