package com.aljodomo.kontis.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Stop Line Direction
 *
 * @author Aljoscha Domonell
 */
@Data
@AllArgsConstructor
public class SLD {
    Stop stop;
    Line line;
    Stop direction;
}
