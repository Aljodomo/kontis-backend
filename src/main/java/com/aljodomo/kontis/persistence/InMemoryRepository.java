package com.aljodomo.kontis.persistence;

import com.aljodomo.kontis.model.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aljoscha Domonell
 */
@Service
@Profile({"test", "dev"})
@Slf4j
public class InMemoryRepository implements ReportRepository {
    public final List<Report> reports;

    @Autowired
    public InMemoryRepository() {
        log.warn("Using non persistent in memory report repository");

        this.reports = new ArrayList<>();
    }

    @Override
    public void create(Report report) {
        this.reports.add(report);
    }

    @Override
    public Report findById(String id) {
        return this.reports.stream()
                .filter(report -> report.getId().toString().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Report> findAll() {
        return this.reports;
    }

    @Override
    public List<Report> findActive() {
        return this.reports
                .stream()
                .filter(report -> report
                        .getTime()
                        .isAfter(ZonedDateTime.now().minus(this.offset)))
                .collect(Collectors.toList());
    }
}
