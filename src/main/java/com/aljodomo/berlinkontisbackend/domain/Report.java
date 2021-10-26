package com.aljodomo.berlinkontisbackend.domain;

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
