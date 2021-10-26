package com.aljodomo.kontis.tagger;

import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Tag> removeDirectionTags(List<Tag> tags, String normalizedMessage) {

        List<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toList());
        Iterator<String> iterator = Arrays.stream(normalizedMessage.split(" ")).iterator();

        while (iterator.hasNext()) {
            String directionKeyword = iterator.next();
            if (iterator.hasNext()) {
                SimilarityScore directionKeywordScore = similarityService.findTop(directionKeywords, directionKeyword);
                if (directionKeywordScore.getScore() > 0.9) {
                    String stopNameWord = iterator.next();
                    int forwardSteps = 2;
                    int step = 0;
                    while (step++ < forwardSteps && iterator.hasNext()) {
                        stopNameWord += iterator.next();
                    }
                    // TODO check the 3 next words and remove the best fitting result.
                    // Maybe other SimilarityStrategy.
                    SimilarityScore nextWordScore = similarityService.findTop(tagNames, stopNameWord);
                    if (nextWordScore.getScore() > 0.5) {
                        tags.removeIf(s -> s.getName().equals(nextWordScore.getKey()));
                    }
                }
            }
        }
        return tags;
    }

    public String removeDirections(List<String> stopNames, String normalizedMessage) {
        List<String> words = Arrays.stream(normalizedMessage.split(" ")).collect(Collectors.toList());
        List<Integer> keyWordIdx = findKeyWordIdx(directionKeywords, words);
        if(!keyWordIdx.isEmpty()) {
            keyWordIdx.forEach(directionKeywordIdx -> {
                int bestMatchingStepCount = findBestMatchingStepCount(words, directionKeywordIdx, stopNames, 3);
                words.subList(directionKeywordIdx, directionKeywordIdx + bestMatchingStepCount).clear();
            });
        }
        return ListUtils.concat(words);
    }

    private int findBestMatchingStepCount(List<String> words,
                                          int directionKeywordIdx,
                                          List<String> stopNames,
                                          int forwardSteps) {
        List<SimilarityScore> stepTopScores = new ArrayList<>();
        for(int i = directionKeywordIdx + 1; i < directionKeywordIdx + 1 + forwardSteps && i < words.size(); i++) {
            String searchString = ListUtils.concat(words.subList(directionKeywordIdx, i));

            SimilarityScore currentStepTopScore = this.similarityService.findTop(stopNames, searchString);
            // TODO minimum precision?
            stepTopScores.add(currentStepTopScore);
        }

        return stepTopScores
                .stream()
                .min((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()))
                .map(similarityScore -> stepTopScores.indexOf(similarityScore) + 1)
                .orElseThrow();
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
