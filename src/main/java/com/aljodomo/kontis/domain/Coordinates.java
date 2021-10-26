package com.aljodomo.kontis.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Aljoscha Domonell
 */
@Data
@AllArgsConstructor
public class Coordinates {
    private final String lon;
    private final String lat;
}
