package com.aljodomo.kontis.telegram;

import com.aljodomo.kontis.gtfs.ReportService;
import com.aljodomo.kontis.model.Report;
import com.aljodomo.kontis.persistence.ReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
@Service
@Slf4j
public class DefaultMessageHandler implements MessageHandler {

    private final ReportService reportService;
    private final ReportRepository db;

    @Autowired
    public DefaultMessageHandler(ReportService reportService, ReportRepository db) {
        this.reportService = reportService;
        this.db = db;
    }

    @Override
    public void handleMessage(String message, ZonedDateTime now) {
        Optional<Report> report = reportService.analyse(message, now);
        if (report.isPresent()) {
            db.create(report.get());
            log.info("Report created: {}", report.get());
        }
    }
}
