package com.aljodomo.kontis.tagger;

import net.ricecode.similarity.JaroStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aljoscha Domonell
 */
@Service
public class StringSimilarityTagger implements Tagger {

    final StringSimilarityService similarityService;

    @Autowired
    public StringSimilarityTagger() {
        SimilarityStrategy strategy = new JaroStrategy();
        similarityService = new WordSimilarityService(strategy);
    }

    @Override
    public Tag find(List<String> tags, String message) {
        /*
         * i = number of search term words
         * check 0...i word groups
         * return probability
         */
        return Tag.of(this.similarityService.findTop(tags, message));
    }

    @Override
    public List<Tag> findAll(List<String> tagList, String message) {
        return this.similarityService
                .scoreAll(tagList, message)
                .stream()
                .sorted((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()))
                .map(Tag::of)
                .collect(Collectors.toList());
    }
}
