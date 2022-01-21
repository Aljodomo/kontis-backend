package com.aljodomo.kontis.nlp.preperation;

import org.springframework.stereotype.Component;

/**
 * @author Aljoscha Domonell
 */
@Component
public class DefaultMessageNormalizer implements MessageNormalizer {

    @Override
    public String normalize(String message) {
        return message
                .toLowerCase()
                .replace("ß", "ss")
                .replace("ü", "ue")
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("/", " ")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
