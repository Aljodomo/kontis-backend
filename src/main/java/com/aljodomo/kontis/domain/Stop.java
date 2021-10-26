package com.aljodomo.kontis.domain;

import lombok.Value;

/**
 * @author Aljoscha Domonell
 */
@Value
public class Stop {
    String name;
    Coordinates coordinates;
}
