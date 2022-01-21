package com.aljodomo.kontis.nlp;

import com.aljodomo.kontis.domain.Line;
import com.aljodomo.kontis.domain.SLD;
import com.aljodomo.kontis.domain.Stop;
import com.aljodomo.kontis.loader.StopLoader;
import com.aljodomo.kontis.nlp.preperation.DirectionRemover;
import com.aljodomo.kontis.nlp.preperation.MessageNormalizer;
import lombok.extern.slf4j.Slf4j;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.StringSimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.aljodomo.kontis.utils.StringUtils.concat;


/**
 * Stop, line, direction matcher.
 *
 *
 * @author Aljoscha Domonell
 */
@Service
@Slf4j
public class SLDBuilder {

    protected static final double PRECISION_THRESHOLD = 0.8;

    private final StringSimilarityService similarityService;
    private final StopLoader stopLoader;
    private final DirectionRemover directionRemover;
    private final MessageNormalizer messageNormalizer;

    @Autowired
    public SLDBuilder(StopLoader stopLoader, StringSimilarityService similarityService, DirectionRemover directionRemover, MessageNormalizer messageNormalizer) {
        this.stopLoader = stopLoader;
        this.similarityService = similarityService;
        this.directionRemover = directionRemover;
        this.messageNormalizer = messageNormalizer;
    }

    /**
     * Analyse the given message and build a {@link SLD StopLineDirection triplet }
     * with its content if provides such.
     *
     * @param message Written message by human reporter.
     * @return {@link SLD StopLineDirection triplet } with the content of the message.
     */
    public Optional<SLD> from(String message) {

        if (message.contains("?")) {
            log.info("Message [{}] seems to be a question and will not be considered as a report", message);
            return Optional.empty();
        }

        String normalizedMessage = messageNormalizer.normalize(message);

        // Needs to be an ArrayList to allow List.sublist().clear() to work.
        ArrayList<String> words = new ArrayList<>(List.of(normalizedMessage.split(" ")));

        Line line = findLine(words);

        List<String> removedDirections = directionRemover.removeDirections(words);

        Stop direction = getFirstDirection(removedDirections);

        String cleanedMessage = concat(words);

        Optional<Stop> stop = findStop(cleanedMessage);

        return stop.map(value -> new SLD(value, line, direction));
    }

    private Optional<Stop> findStop(String cleanedMessage) {
        SimilarityScore bestMatch = similarityService.findTop(stopLoader.getStopNames(), cleanedMessage);

        if (bestMatch.getScore() > PRECISION_THRESHOLD) {
            return Optional.of(stopLoader.getStops().get(bestMatch.getKey()));
        } else {
            log.info("No stop found with sufficient precision for message [{}]", cleanedMessage);
            return Optional.empty();
        }
    }

    private Line findLine(List<String> words) {
        List<Line> lines = words
                .stream()
                .filter(s -> s.matches("[sSuU]\\d+"))
                .map(s -> new Line(s.replace("u", "U").replace("s", "S")))
                .collect(Collectors.toList());

        if (lines.isEmpty()) {
            log.debug("No lines were identified [{}]", concat(words));
            return null;
        } else {
            if (lines.size() > 1) {
                log.info("More then one line was identified but only the first one will be used [{}]", concat(words));
            }

            return lines.get(0);
        }
    }

    private Stop getFirstDirection(List<String> directions) {
        if (directions.isEmpty()) {
            log.debug("No directions were identified [{}]", directions);
            return null;
        } else {
            if (directions.size() > 1) {
                log.info("More then one direction was identified but only the first one will be used [{}]", directions);
            }

            SimilarityScore bestMatch = similarityService.findTop(stopLoader.getStopNames(), directions.get(0));
            if (bestMatch.getScore() >= PRECISION_THRESHOLD) {
                return stopLoader.getStops().get(bestMatch.getKey());
            } else {
                return null;
            }
        }
    }
}
