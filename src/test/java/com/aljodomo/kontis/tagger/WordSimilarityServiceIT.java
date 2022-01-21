package com.aljodomo.kontis.tagger;

import com.aljodomo.kontis.nlp.preperation.WordSimilarityService;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.StringSimilarityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
class WordSimilarityServiceIT {
    StringSimilarityService tagger = new WordSimilarityService();
    List<String> matches = List.of("Alexanderplatz",
            "Schlossstraße",
            "Rathaus Steglitz",
            "Berliner Straße");

    @Test
    void testStopExtraction() {
        String message = "2 männlich gelesen U9 richtung Rathaus Steglitz alex raus";
        SimilarityScore t = this.tagger.findTop(matches, message);
        Assertions.assertEquals("Rathaus Steglitz", t.getKey());
    }
}
