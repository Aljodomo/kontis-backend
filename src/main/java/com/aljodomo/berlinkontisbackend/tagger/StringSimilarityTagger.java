package com.aljodomo.berlinkontisbackend.tagger;

import net.ricecode.similarity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Aljoscha Domonell
 */
@Service
public class StringSimilarityTagger implements Tagger {

    final StringSimilarityService similarityService;

    @Autowired
    public StringSimilarityTagger() {
        SimilarityStrategy strategy = new JaroStrategy();
        similarityService = new SubStringSimilarityService(strategy);
    }

    @Override
    public Tag find(List<String> tags, String message) {
        /**
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
