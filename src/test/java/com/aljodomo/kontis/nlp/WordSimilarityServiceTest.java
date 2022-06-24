package com.aljodomo.kontis.nlp;

import net.ricecode.similarity.StringSimilarityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aljoscha Domonell
 */
class WordSimilarityServiceTest {
    StringSimilarityService service = new WordSimilarityService();
    String message = "2 männlich gelesen U9 richtung Rathaus Steglitz alex raus";
    List<String> matches = List.of("Alexanderplatz",
            "Schlossstraße",
            "Rathaus Steglitz",
            "Berliner Straße");

    @Test
    void testEmptyDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> service.findTop(new ArrayList<>(), ""));
        Assertions.assertDoesNotThrow(() -> service.findTop(new ArrayList<>(), "asd"));
    }

    @Test
    void testHighPrecision() {
        Assertions.assertTrue(service.score("Rathaus Steglitz", message) > Precision.HIGH);
        Assertions.assertTrue(service.score("männlich", message) > Precision.HIGH);
        Assertions.assertTrue(service.score("maennlich", message) > Precision.HIGH);
        Assertions.assertTrue(service.score("gelesen", message) > Precision.HIGH);
        Assertions.assertTrue(service.score("männlich gelesen", message) > Precision.HIGH);
    }

    @Test
    void testOnlyRathausSteglitzHighPrecision() {
        var res = service.scoreAll(matches, message);
        Assertions.assertTrue(res.get(0).getScore() > Precision.HIGH);
        res.remove(0);

        res.forEach(similarityScore -> Assertions.assertTrue(similarityScore.getScore() < Precision.HIGH));
    }

}
