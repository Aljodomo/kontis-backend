package com.aljodomo.kontis;

import com.aljodomo.kontis.domain.SLD;
import com.aljodomo.kontis.nlp.preperation.MessageNormalizer;
import com.aljodomo.kontis.nlp.SLDParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
@SpringBootTest
class SLDMatcherIT {

    @Autowired
    SLDParser matcher;

    @Autowired
    MessageNormalizer normalizer;

    @Test
    void testMessage1() {
        String message = "2 männlich gelesen U9 richtung Rathaus Steglitz schloßstr raus";
        sldMatches("schlossstr", "U9", "rathaus steglitz", message);
    }

    @Test
    void testMessage2() {
        String message = "Ring 41, die steigen gleich in Tempelhof aus\n";
        sldMatches("tempelhof", null, null, message);
    }

    @Test
    void testMessage3() {
        String message = "Zoologischer 3 kontrolletis";
        sldMatches("zoologischer garten", null, null, message);
    }

    @Test
    void testMessage4() {
        String message = "U9 direction rathaus steglitz, now at zoologischere garten";
        sldMatches("zoologischer garten", "U9", "rathaus steglitz", message);
    }

    @Test
    void testMessage5() {
        String message = "U9 Hansaplatz richtung Osloer 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        sldMatches("hansaplatz", "U9", "osloer str", message);
    }

    @Test
    void testMessage6() {
        String message = "3 kontrolletis grade am Treptower park ausgestiegen. Kamen aus der Ringbahn Richtung Frankfurter Allee.";
        sldMatches("treptower park", null, "frankfurter allee", message);
    }

    @Test
    void testMessage7() {
        String message = "Now Karl Marx str they are both in black one has glasses";
        sldMatches("karlmarxstr", null, null, message);
    }

    private void sldMatches(String stop, String line, String direction, String message) {
        Optional<SLD> sld = this.matcher.parse(message);
        Assertions.assertTrue(sld.isPresent());
        Assertions.assertEquals(stop, sld.get().getStop().getName());
        if(line != null) {
            Assertions.assertEquals(line, sld.get().getLine().getName());
        } else {
            Assertions.assertNull(sld.get().getLine());
        }
        if(direction != null) {
            Assertions.assertEquals(direction, sld.get().getDirection().getName());
        } else {
            Assertions.assertNull(sld.get().getDirection());
        }
    }
}
