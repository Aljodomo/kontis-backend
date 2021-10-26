package com.aljodomo.kontis.domain;

import lombok.Data;

import java.time.Instant;

/**
 * @author Aljoscha Domonell
 */
@Data
public class Report {
    final Stop stop;
    final Instant instant;
}
