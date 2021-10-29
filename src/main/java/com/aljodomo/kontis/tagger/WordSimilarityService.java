package com.aljodomo.kontis.tagger;

import net.ricecode.similarity.DescendingSimilarityScoreComparator;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aljoscha Domonell
 */
public class WordSimilarityService implements StringSimilarityService {

    protected SimilarityStrategy strategy;

    /**
     * Creates a similarity calculator instance.
     *
     * @param strategy The similarity strategy to use when calculating similarity scores.
     */
    public WordSimilarityService(SimilarityStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public List<SimilarityScore> scoreAll(List<String> features, String target) {
        ArrayList<SimilarityScore> scores = new ArrayList<>();

        for (String feature : features) {
            double score = this.score(feature, target);
            scores.add(new SimilarityScore(feature, score));
        }

        return scores;
    }

    @Override
    public double score(String feature, String target) {
        int featureWordCount = feature.split(" ").length;
        List<String> messageWords = Arrays.stream(target.split(" ")).collect(Collectors.toList());
        double finalScore = 0;
        // Count symbols from feature and to forward for at least this number in symbols
        for (int groupSize = 1; groupSize <= featureWordCount; groupSize++) {
            for (int i = 0; i < messageWords.size() - groupSize; i++) {
                String subTarget = ListUtils.concat(messageWords.subList(i, i + groupSize));
                double tmpScore = strategy.score(feature, subTarget);
                finalScore = Math.max(tmpScore, finalScore);
            }
        }
        return finalScore;
    }

    @Override
    public SimilarityScore findTop(List<String> features, String target) {
        return this.findTop(features, target, new DescendingSimilarityScoreComparator());
    }

    @Override
    public SimilarityScore findTop(List<String> features, String target, Comparator<SimilarityScore> comparator) {
        if (features.isEmpty()) {
            return null;
        }
        List<SimilarityScore> scores = this.scoreAll(features, target);
        scores.sort(comparator);
        return scores.get(0);
    }
}
