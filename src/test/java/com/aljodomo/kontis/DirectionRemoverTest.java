package com.aljodomo.kontis;

import com.aljodomo.kontis.loader.StopLoader;
import com.aljodomo.kontis.nlp.preperation.DirectionRemover;
import com.aljodomo.kontis.nlp.preperation.MessageNormalizer;
import com.aljodomo.kontis.utils.StringUtils;
import net.ricecode.similarity.DiceCoefficientStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aljoscha Domonell
 */
@SpringBootTest
class DirectionRemoverTest {
    private final List<String> stopNames;
    private final MessageNormalizer messageNormalizer;
    DirectionRemover directionRemover;

    @Autowired
    public DirectionRemoverTest(StopLoader stopLoader, MessageNormalizer messageNormalizer) {
        this.directionRemover = new DirectionRemover(new DiceCoefficientStrategy(), stopLoader); // osloer str = 0.43
        this.stopNames = stopLoader.getStopNames();
        this.messageNormalizer = messageNormalizer;
    }

    @Test
    void testDirectionRemoval() {
        String message = "U9 Hansaplatz richtung Osloer 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        String expected = "U9 Hansaplatz 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
        String nMessage = this.messageNormalizer.normalize(message);
        ArrayList<String> words = new ArrayList<>(List.of(nMessage.split(" ")));
        List<String> removeDirections = this.directionRemover.removeDirections(words);
        Assertions.assertEquals(messageNormalizer.normalize(expected), StringUtils.concat(words));
    }
}
