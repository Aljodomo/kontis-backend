package com.aljodomo.kontis.controller;

import com.aljodomo.kontis.gtfs.GTFSService;
import com.aljodomo.kontis.gtfs.ReportService;
import com.aljodomo.kontis.model.Report;
import com.aljodomo.kontis.persistence.InMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aljoscha Domonell
 */
@RestController
@Profile("dev")
@CrossOrigin
public class DataController {

    @Autowired
    InMemoryRepository inMemoryDB;

    @Autowired
    ReportService reportService;

    @Autowired
    GTFSService gtfsService;

    @GetMapping("/activeReports")
    public ResponseEntity<List<Report>> activeReports() {
        return ResponseEntity.ok().body(this.inMemoryDB.findActive());
    }

    @PostMapping("/future")
    public ResponseEntity<List<Report>> buildFuture(@RequestBody String message) {
        ZonedDateTime time = ZonedDateTime.now();
        var r = reportService.analyse(message, time);

        return r.map(report -> ResponseEntity.ok().body(
                        gtfsService.findReachable(report.getStopTime(), time, Duration.of(10, ChronoUnit.MINUTES))
                                .stream()
                                .map(stopTime -> Report.build(message, stopTime, time))
                                .collect(Collectors.toList())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/reports")
    public ResponseEntity<Report> buildReport(@RequestBody String message) {
        var r = reportService.analyse(message, ZonedDateTime.now());

        return r.map(report -> ResponseEntity.ok().body(report))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
