package com.aljodomo.kontis.tagger;

import com.aljodomo.kontis.domain.Stop;
import com.aljodomo.kontis.loader.VbbStopLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
class MatcherIT {

    private final StopMatcher matcher;

    public MatcherIT() throws IOException {
        MessageNormalizer normalizer = new DefaultMessageNormalizer();
        this.matcher = new StopMatcher(new VbbStopLoader(normalizer), new StringSimilarityTagger(), new DirectionRemover(), normalizer);
    }

    @Test
    void testMessage1() {
        String message = "2 männlich gelesen U9 richtung Rathaus Steglitz schloßstr raus";
        matchFound("schlossstr", message);
    }

    @Test
    void testMessage2() {
        String message = "Ring 41, die steigen gleich in Tempelhof aus\n";
        matchFound("tempelhof", message);
    }

    @Test
    void testMessage3() {
        String message = "Zoologischer 3 kontrolletis";
        matchFound("zoologischer garten", message);
    }

    @Test
    void testMessage4() {
        String message = "U9 direction rathaus steglitz, now at zoologischere garten";
        matchFound("zoologischer garten", message);
    }

    @Test
    void testMessage5() {
        String message = "U9 Hansaplatz richtung Osloer 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        matchFound("hansaplatz", message);
    }

    @Test
    void testMessage6() {
        String message = "3 kontrolletis grade am Treptower park ausgestiegen. Kamen aus der Ringbahn Richtung Frankfurter Allee.";
        matchFound("treptower park", message);
    }

    @Test
    void testMessage7() {
        // TODO case karlmarxstr. Original Feature has less words then message feature.
        String message = "Now Karl Marx str they are both in black one has glasses";
        matchFound("karl marx strasse", message);
    }

    private void matchFound(String expected, String message) {
        Optional<Stop> stop = this.matcher.match(message);
        Assertions.assertTrue(stop.isPresent());
        Assertions.assertEquals(expected, stop.get().getName());
    }
}
