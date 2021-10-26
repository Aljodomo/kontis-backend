package com.aljodomo.kontis.tagger;

import com.aljodomo.kontis.domain.Stop;
import com.aljodomo.kontis.loader.StopLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
@Service
public class StopMatcher {
    final Tagger tagger;
    final StopLoader stopLoader;
    private final DirectionRemover directionRemover;
    private final MessageNormalizer messageNormalizer;

    @Autowired
    public StopMatcher(StopLoader stopLoader, Tagger tagger, DirectionRemover directionRemover, MessageNormalizer messageNormalizer) {
        this.stopLoader = stopLoader;
        this.tagger = tagger;
        this.directionRemover = directionRemover;
        this.messageNormalizer = messageNormalizer;
    }

    public Optional<Stop> match(String message) {
        List<String> stationNames = new ArrayList<>(stopLoader.getStations().keySet());

        String normalizedMessage = this.messageNormalizer.normalize(message);

        String cleanedMessage = this.directionRemover.removeDirections(stationNames, normalizedMessage);

        List<Tag> tags = this.tagger.findAll(stationNames, cleanedMessage);

        Tag bestMatch = tags.get(0);
        
        if(bestMatch.getPrecision() > 0.2) {
            return Optional.ofNullable(stopLoader.getStations().get(bestMatch.getName()));
        } else {
            return Optional.empty();
        }
    }

}
