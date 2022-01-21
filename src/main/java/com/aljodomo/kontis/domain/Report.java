package com.aljodomo.kontis.domain;

import lombok.Value;

import java.time.Instant;

/**
 * @author Aljoscha Domonell
 */
@Value
public class Report {
    String originalMessage;
    SLD sld;
    Instant time;
}
