package com.aljodomo.kontis.nlp.preperation;

import com.aljodomo.kontis.loader.StopLoader;
import com.aljodomo.kontis.utils.StringUtils;
import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aljoscha Domonell
 */
@Service
public class DirectionRemover {
    private final StringSimilarityServiceImpl similarityService;
    private final List<String> directionKeywords;
    private final StopLoader stopLoader;

    @Autowired
    public DirectionRemover(StopLoader stopLoader) {
        this(new DiceCoefficientStrategy(), stopLoader);
    }

    public DirectionRemover(SimilarityStrategy strategy, StopLoader stopLoader) {
        similarityService = new StringSimilarityServiceImpl(strategy);
        this.stopLoader = stopLoader;
        this.directionKeywords = List.of("richtung", "nach", "direction");
    }

    /**
     * Search for direction keywords and remove with trailing {@link StopLoader#getStopNames() stopNames}.
     * Return the cleaned message.
     *
     * @param words {@link MessageNormalizer#normalize(String) normalized} message split into words.
     *              Needs to be an {@link ArrayList} that returns a mutable list on {@link ArrayList#subList(int, int)}.
     * @return List of all removed {@link StopLoader#getStopNames() stopNames};
     */
    public List<String> removeDirections(ArrayList<String> words) {
        List<Integer> keyWordIdx = findKeyWordIdx(directionKeywords, words);
        List<String> directions = new ArrayList<>();
        List<String> stopNames = this.stopLoader.getStopNames();
        if (!keyWordIdx.isEmpty()) {
            keyWordIdx.forEach(directionKeywordIdx -> {
                int bestMatchingStepCount = findBestMatchingStepCount(words, directionKeywordIdx, stopNames, 3);
                List<String> directionWords = words.subList(directionKeywordIdx, directionKeywordIdx + 1 + bestMatchingStepCount);
                // Remove the direction key word itself to keep only the trailing words.
                String directionWithoutKeyWord = StringUtils.concat(directionWords.subList(1, directionWords.size()));
                directions.add(directionWithoutKeyWord);
                words.subList(directionKeywordIdx, directionKeywordIdx + bestMatchingStepCount).clear();
            });
        }
        return directions;
    }

    /**
     * Given the message split into words, look at the directionsKeywordIdx
     * and calculate with how many steps you will
     * get the best {@link SimilarityScore match} for a stop name provided in the stopNames list.
     *
     * @param words               Message split into words.
     * @param directionKeywordIdx Index of the direction keyword to inspect.
     * @param stopNames           Stop names to look for a {@link SimilarityScore match}.
     * @param forwardSteps        Maximum number of forward steps allowed.
     * @return Number of forward steps with the best {@link SimilarityScore match} .
     */
    private int findBestMatchingStepCount(List<String> words,
                                          int directionKeywordIdx,
                                          List<String> stopNames,
                                          int forwardSteps) {
        List<SimilarityScore> stepTopScores = new ArrayList<>();
        for (int i = directionKeywordIdx + 1; i < directionKeywordIdx + 1 + forwardSteps && i < words.size(); i++) {
            String searchString = StringUtils.concat(words.subList(directionKeywordIdx, i));

            SimilarityScore currentStepTopScore = this.similarityService.findTop(stopNames, searchString);
            // TODO minimum precision?
            stepTopScores.add(currentStepTopScore);
        }

        return stepTopScores
                .stream()
                .min((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()))
                .map(similarityScore -> stepTopScores.indexOf(similarityScore) + 1)
                .orElse(0);
    }

    private List<Integer> findKeyWordIdx(List<String> directionKeywords, List<String> words) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            SimilarityScore directionKeywordScore = similarityService.findTop(directionKeywords, words.get(i));
            if (directionKeywordScore.getScore() > 0.8) {
                indexes.add(i);
            }
        }
        return indexes;
    }
}
