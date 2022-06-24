package com.aljodomo.kontis.nlp;

import com.aljodomo.kontis.utils.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aljoscha Domonell
 */
class DirectionRemoverTest {
    private final DirectionRemover directionRemover = new DirectionRemover();

    List<String> stops = List.of("Alexanderplatz",
            "Schlossstraße",
            "Rathaus Steglitz",
            "Frankfurter Allee",
            "Berliner Straße",
            "Osloer Straße");

    @Test
    void testOsloerStrasse() {
        String message = "U9 Hansaplatz richtung Osloer mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        String expected = "U9 Hansaplatz mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        test(message, expected, List.of("Osloer"));
    }

    @Test
    void testRathausSteglitz() {
        String message = "2 männlich gelesen U9 richtung Rathaus Steglitz alex raus";
        String expected = "2 männlich gelesen U9 alex raus";
        List<String> expectedDirections = List.of("Rathaus Steglitz");
        test(message, expected, expectedDirections);
    }

    @Test
    void testKarlMarxWithNoDirections() {
        String message = "Now Karl Marx str they are both in black one has glasses";
        test(message, message, new ArrayList<>());
    }

    @Test
    void testFrankfurterAllee() {
        String message = "kontrolletis grade am Treptower park ausgestiegen Kamen aus der Ringbahn Richtung Frankfurter Allee";
        String expected = "kontrolletis grade am Treptower park ausgestiegen Kamen aus der Ringbahn";
        test(message, expected, List.of("Frankfurter Allee"));
    }

    private void test(String message, String expected, List<String> expectedDirections) {
        ArrayList<String> words = new ArrayList<>(List.of(message.split(" ")));
        List<String> removedDirections = this.directionRemover.removeDirections(words, stops);
        Assertions.assertEquals(expected, StringUtils.concat(words));
        Assertions.assertEquals(removedDirections, expectedDirections);
    }
}
