package com.aljodomo.kontis.tagger;

import com.aljodomo.kontis.loader.StopLoader;
import net.ricecode.similarity.DiceCoefficientStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

/**
 * @author Aljoscha Domonell
 */
@SpringBootTest
class DirectionRemoverTest {
    private final ArrayList<String> stopNames;
    private final MessageNormalizer messageNormalizer;
    DirectionRemover directionRemover;

    @Autowired
    public DirectionRemoverTest(StopLoader stopLoader, MessageNormalizer messageNormalizer) {
        this.directionRemover = new DirectionRemover(new DiceCoefficientStrategy()); // osloer str = 0.43
        this.stopNames = new ArrayList<>(stopLoader.getStations().keySet());
        this.messageNormalizer = messageNormalizer;
    }

    @Test
    void testDirectionRemoval() {
        String message = "U9 Hansaplatz richtung Osloer 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        String expected = "U9 Hansaplatz 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        String nMessage = this.messageNormalizer.normalize(message);
        String clearedMessage = this.directionRemover.removeDirections(stopNames, nMessage);
        Assertions.assertEquals(messageNormalizer.normalize(expected), clearedMessage);
    }
}
