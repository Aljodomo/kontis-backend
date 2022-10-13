package com.aljodomo.kontis;

import com.aljodomo.kontis.gtfs.GTFSService;
import com.aljodomo.kontis.gtfs.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Aljoscha Domonell
 */
@SpringBootTest
@ActiveProfiles({"dev", "inMemory"})
@Slf4j
class MessageIT {

    @Autowired
    ReportService reportService;

    @Autowired
    GTFSService gtfsService;

    void testComplete(String message, String timeS, String stop) {
        var time = ZonedDateTime.of(LocalDateTime.parse(timeS), TimeZone.getDefault().toZoneId());
        var report = reportService.analyse(message, time);

        assertTrue(report.isPresent(),
                "No report was build even tho all necessary information is present");

        log.info(report.get().toString());

        assertNotNull(report.get().getStopTimeId(),
                "There was no specified StopTimeId present even tho all necessary information is present");
        assertEquals(stop, report.get().getStopName(),
                "The expected stop name is not the one identified");
    }

    void testPartial(String message, String timeS, String stop) {
        var time = ZonedDateTime.of(LocalDateTime.parse(timeS), TimeZone.getDefault().toZoneId());
        var report = reportService.analyse(message, time);

        assertTrue(report.isPresent());
        assertNull(report.get().getStopTimeId());
        assertEquals(stop, report.get().getStopName());
    }

    void test(String message, String timeS, int exStopTimeId) {
        var time = ZonedDateTime.of(LocalDateTime.parse(timeS), TimeZone.getDefault().toZoneId());
        var expected = gtfsService.getStore().getStopTimeForId(exStopTimeId);
        var report = reportService.analyse(message, time);

        assertTrue(report.isPresent());

        Assertions.assertEquals(expected.getId(), report.get().getStopTimeId());
    }

    /**
     * 1362073
     */
    @Test
    void complete_message_expect_schlossstr() {
        testComplete("2 männlich gelesen U9 richtung Rathaus Steglitz schloßstr raus", "2022-02-21T17:39:00", "U Schloßstr. (Berlin)");
    }

    @Test
    void partial_message_expect_tempelhof() {
        testPartial("Ring 41, die steigen gleich in Tempelhof aus\n", "2022-02-21T17:39:00", "S+U Tempelhof (Berlin)");
    }

    @Test
    void partial_message_expect_zoologischer() {
        testPartial("Zoologischer 3 kontrolletis", "2022-02-21T17:39:00", "S+U Zoologischer Garten Bhf (Berlin)");
    }

    @Test
    void complete_message_expect_zoologischer2() {
        testComplete("U9 direction rathaus steglitz, now at zoologischere garten", "2022-02-21T17:39:00", "S+U Zoologischer Garten Bhf (Berlin)");
    }

    @Test
    void complete_message_expect_hansaplatz() {
        testComplete("U9 Hansaplatz richtung Osloer 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare", "2022-02-21T17:39:00", "U Hansaplatz (Berlin)");
    }

    @Test
    void complete_message_expect_nordbahnhof() {
        testComplete("s25 richtung südkreuz jetzt nordbahnhof", "2022-02-21T17:39:00", "S Nordbahnhof (Berlin)");
    }

    /**
     * FIXME fails because "ringbahn" is not mapped to S42 / S41
     */
    @Test
    void complete_message_expect_treptower() {
        testComplete("3 kontrolletis grade am Treptower park ausgestiegen. Kamen aus der Ringbahn Richtung Frankfurter Allee.", "2022-02-21T17:39:00", "S Treptower Park (Berlin)");
    }
}
