package com.aljodomo.kontis.nlp;

import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.aljodomo.kontis.utils.StringUtils.concat;

/**
 * Helper to find and remove headSigns from a message
 *
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
        this.similarityService = new StringSimilarityServiceImpl(strategy);
        this.directionKeywords = List.of("richtung", "nach", "direction");
    }

    /**
     * Search for direction keywords and remove with trailing stopNames.
     * Return the cleaned message.
     *
     * @param message {@link MessageNormalizer#normalize(String) normalized} message split into words.
     * @param targetHeadSigns List of headSigns to scan the message for
     * @return List of all removed HeadSigns;
     */
    public List<String> cutHeadSignsAndKeywords(List<String> message, List<String> targetHeadSigns) {
        List<String> headSigns = new ArrayList<>();

        List<Integer> directionKeyIndexes = findKeyWordIndexes(message);
        for(int keywordIdx : directionKeyIndexes) {
            cutHeadSign(message, keywordIdx, targetHeadSigns)
                    .ifPresent(headSigns::add);
            message.remove(keywordIdx);
        }

        return headSigns;
    }

    private Optional<String> cutHeadSign(List<String> words, int keywordIdx, List<String> targetHeadSigns) {
        int maxForwardSteps = 3;

        List<SimilarityScore> stepScores = new ArrayList<>();
        int firstIndex = keywordIdx + 1;
        for (int i = 1; i <= maxForwardSteps; i++) {
            int currentIndex = firstIndex + i;
            if (currentIndex >= words.size() + 1) {
                break;
            }
            String relevantSubstring = concat(words.subList(firstIndex, currentIndex));
            stepScores.add(similarityService.findTop(targetHeadSigns, relevantSubstring));
        }

        Optional<SimilarityScore> maxScoreO = stepScores.stream()
                .filter(similarityScore -> similarityScore.getScore() > Precision.MEDIUM)
                .max(Comparator.comparingDouble(SimilarityScore::getScore));

        if(maxScoreO.isPresent()) {
            SimilarityScore maxScore = maxScoreO.get();

            int maxScoreSteps = stepScores.indexOf(maxScore) + 1;
            clear(words, keywordIdx + 1, maxScoreSteps);

            return Optional.of(maxScore.getKey());
        } else {
            return Optional.empty();
        }
    }

    private static void clear(List<String> words, int begin, int end) {
        for(int i = 0; i < end; i++) {
            words.remove(begin);
        }
    }

    private List<Integer> findKeyWordIndexes(List<String> words) {
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
