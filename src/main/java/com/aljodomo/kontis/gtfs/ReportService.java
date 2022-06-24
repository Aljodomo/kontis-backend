package com.aljodomo.kontis.gtfs;

import com.aljodomo.kontis.model.Report;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
public interface ReportService {

    /**
     * Analyse the given massage and tries to extract GTFS information encapsuldated in the returned {@link Report}.
     *
     * @param message Human-readable massage.
     * @param time    When to massage was created.
     * @return
     */
    Optional<Report> analyse(String message, ZonedDateTime time);
}
