package com.aljodomo.berlinkontisbackend.domain;

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
