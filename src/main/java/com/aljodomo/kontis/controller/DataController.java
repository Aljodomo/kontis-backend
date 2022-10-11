package com.aljodomo.kontis.controller;

import com.aljodomo.kontis.gtfs.ReportService;
import com.aljodomo.kontis.model.Report;
import com.aljodomo.kontis.persistence.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

/**
 * @author Aljoscha Domonell
 */
@RestController
@Profile("dev")
@CrossOrigin
public class DataController {

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    ReportService reportService;

    @PostMapping("/reports")
    public ResponseEntity<Report> buildReport(@RequestBody String message) {
        var r = reportService.analyse(message, ZonedDateTime.now());

        return r.map(report -> ok().body(report))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/reports/save")
    public ResponseEntity<Report> saveReport(@RequestBody String message) {
        var r = reportService.analyse(message, ZonedDateTime.now());

        r.ifPresent(reportRepository::create);

        return r.map(ResponseEntity::ok)
                .orElseGet(() -> badRequest().build());
    }


}
