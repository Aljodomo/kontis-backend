package com.aljodomo.kontis.nlp.preperation;

import com.aljodomo.kontis.utils.StringUtils;
import net.ricecode.similarity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aljoscha Domonell
 */
@Service
public class WordSimilarityService implements StringSimilarityService {

    protected SimilarityStrategy strategy;

    @Autowired
    public WordSimilarityService() {
        strategy = new JaroStrategy();
    }

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

        scores.sort((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

        return scores;
    }

    @Override
    public double score(String feature, String target) {
        int featureWordCount = feature.split(" ").length;
        List<String> messageWords = Arrays.stream(target.split(" ")).collect(Collectors.toList());
        double finalScore = 0;
        int currentCharCount = 0;
        // Count symbols from feature and to forward for at least this number in symbols
        for (int i = 0; i < messageWords.size(); i++) {
            for (int groupSize = 1; i + groupSize <= messageWords.size() &&
                    (groupSize <= featureWordCount || currentCharCount < feature.length()); groupSize++) {
                String subTarget = StringUtils.concat(messageWords.subList(i, i + groupSize));
                // Count characters without whitespaces
                currentCharCount = subTarget.replace(" ", "").length();
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
