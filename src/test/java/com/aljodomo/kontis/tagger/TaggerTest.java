package com.aljodomo.kontis.tagger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
class TaggerTest {
    Tagger tagger = new StringSimilarityTagger();
    List<String> tags = List.of("Alexanderplatz",
            "Schlossstraße",
            "Rathaus Steglitz",
            "Berliner Straße");

    @Test
    void testStopExtraction() {
        String message = "2 männlich gelesen U9 richtung Rathaus Steglitz alex raus";
        List<Tag> t = this.tagger.findAll(tags, message);
        Assertions.assertEquals("Rathaus Steglitz", t.get(0).getName());
    }
}
