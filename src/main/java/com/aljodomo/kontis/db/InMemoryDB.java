package com.aljodomo.kontis.db;

import com.aljodomo.kontis.domain.Report;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aljoscha Domonell
 */
@Component
public class InMemoryDB {
    final List<Report> reports;
    final Duration offset;

    public InMemoryDB() {
        this.reports = new ArrayList<>();
        this.offset = Duration.ofMinutes(30);
    }

    public void addReport(Report report) {
        this.reports.add(report);
    }

    public List<Report> getAllReports() {
        return this.reports;
    }

    public List<Report> getActiveReports() {
        return this.reports
                .stream()
                .filter(report -> report
                        .getInstant()
                        .isAfter(Instant.now().minus(this.offset)))
                .collect(Collectors.toList());
    }
}
