package com.aljodomo.kontis.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

/**
 * @author Aljoscha Domonell
 */
@Value
@AllArgsConstructor
public class Coordinates {
    String lon;
    String lat;
}
