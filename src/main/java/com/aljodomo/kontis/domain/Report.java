package com.aljodomo.kontis.domain;

import lombok.Data;
import lombok.Value;

import java.time.Instant;

/**
 * @author Aljoscha Domonell
 */
@Value
public class Report {
    Stop stop;
    Instant instant;
}
