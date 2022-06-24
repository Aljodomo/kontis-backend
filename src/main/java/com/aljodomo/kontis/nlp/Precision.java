package com.aljodomo.kontis.nlp;

/**
 * Defines constants to identify the precision of a {@link net.ricecode.similarity.SimilarityScore}.
 * Is based on no formal definitions.
 *
 * @author Aljoscha Domonell
 */
public class Precision {

    /**
     * High chance that the target string is the feature string.
     */
    public static final double HIGH = 0.8;

    /**
     * Medium chance that the target string is the feature string.
     */
    public static final double MEDIUM = 0.5;

    /**
     * Low chance that the target string is the feature string.
     */
    public static final double LOW = 0.2;

    private Precision() {
        // Enforce static usage
    }
}
