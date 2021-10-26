package com.aljodomo.berlinkontisbackend.domain;

import lombok.Data;

/**
 * @author Aljoscha Domonell
 */
@Data
public class Stop {
    final String name;
    final Coordinates coordinates;
}
