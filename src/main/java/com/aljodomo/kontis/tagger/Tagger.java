package com.aljodomo.kontis.tagger;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
public interface Tagger {
    Tag find(List<String> tags, String message);

    List<Tag> findAll(List<String> tags, String message);
}
