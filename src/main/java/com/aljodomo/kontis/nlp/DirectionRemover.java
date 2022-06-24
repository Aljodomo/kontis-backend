package com.aljodomo.kontis.nlp;

import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.aljodomo.kontis.utils.StringUtils.concat;

/**
 * @author Aljoscha Domonell
 */
@Service
public class DirectionRemover {
    private final StringSimilarityServiceImpl similarityService;
    private final List<String> directionKeywords;

    @Autowired
    public DirectionRemover() {
        this(new DiceCoefficientStrategy());
    }

    public DirectionRemover(SimilarityStrategy strategy) {
        similarityService = new StringSimilarityServiceImpl(strategy);
        this.directionKeywords = List.of("richtung", "nach", "direction");
    }

    /**
     * Search for direction keywords and remove with trailing stopNames.
     * Return the cleaned message.
     *
     * @param words {@link MessageNormalizer#normalize(String) normalized} message split into words.
     *              Needs to be an {@link ArrayList} that returns a mutable list on {@link ArrayList#subList(int, int)}.
     * @return List of all removed stopNames;
     */
    public List<String> removeDirections(ArrayList<String> words, List<String> stopNames) {
        List<Integer> keyWordIdx = findKeyWordIdx(directionKeywords, words);
        List<String> directions = new ArrayList<>();
        if (!keyWordIdx.isEmpty()) {
            keyWordIdx.forEach(directionKeywordIdx -> {
                int bestMatchingStepCount = findBestMatchingStepCount(words, directionKeywordIdx, stopNames, 3);
                List<String> directionWords = words.subList(directionKeywordIdx + 1, directionKeywordIdx + 1 + bestMatchingStepCount);
                // Remove the direction key word itself to keep only the trailing words.
                String direction = concat(directionWords);
                directions.add(direction);
                words.subList(directionKeywordIdx, directionKeywordIdx + 1 + bestMatchingStepCount).clear();
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
     * @param maxForwardSteps     Maximum number of forward steps allowed.
     * @return Number of forward steps with the best {@link SimilarityScore match} .
     */
    private int findBestMatchingStepCount(List<String> words,
                                          int directionKeywordIdx,
                                          List<String> stopNames,
                                          int maxForwardSteps) {

        SimilarityScore[] stepScores = new SimilarityScore[maxForwardSteps + 1];
        int firstIndex = directionKeywordIdx + 1;
        for (int i = 0; i <= maxForwardSteps; i++) {
            int currentIndex = firstIndex + i;
            if (currentIndex >= words.size() + 1) {
                break;
            }
            String relevantSubstring = concat(words.subList(firstIndex, currentIndex));
            SimilarityScore substringScore = this.similarityService.findTop(stopNames, relevantSubstring);
            if (substringScore.getScore() > Precision.LOW) {
                stepScores[i] = substringScore;
            }
        }

        int maxScoreIdx = 0;
        for (int i = 0; i < stepScores.length; i++) {
            SimilarityScore score = stepScores[i];
            if (score == null) {
                continue;
            }
            if (stepScores[maxScoreIdx] == null) {
                maxScoreIdx = i;
                continue;
            }
            if (score.getScore() > stepScores[maxScoreIdx].getScore()) {
                maxScoreIdx = i;
            }
        }

        return maxScoreIdx;
    }

    private List<Integer> findKeyWordIdx(List<String> directionKeywords, List<String> words) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            SimilarityScore directionKeywordScore = similarityService.findTop(directionKeywords, words.get(i));
            if (directionKeywordScore.getScore() > Precision.HIGH) {
                indexes.add(i);
            }
        }
        return indexes;
    }
}
