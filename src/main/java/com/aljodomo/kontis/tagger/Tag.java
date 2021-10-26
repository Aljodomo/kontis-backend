package com.aljodomo.kontis.tagger;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.ricecode.similarity.SimilarityScore;

/**
 * @author Aljoscha Domonell
 */
@Data
@AllArgsConstructor
public class Tag {
    private String name;
    private double precision;

    public static Tag of(SimilarityScore similarityScore) {
        return new Tag(similarityScore.getKey(), similarityScore.getScore());
    }
}
