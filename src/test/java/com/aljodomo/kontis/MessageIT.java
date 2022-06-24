package com.aljodomo.kontis;

import com.aljodomo.kontis.gtfs.DefaultReportService;
import com.aljodomo.kontis.gtfs.GTFSFilterProps;
import com.aljodomo.kontis.gtfs.GTFSService;
import com.aljodomo.kontis.gtfs.ReportService;
import com.aljodomo.kontis.model.Coordinates;
import com.aljodomo.kontis.model.Report;
import com.aljodomo.kontis.nlp.DirectionRemover;
import com.aljodomo.kontis.nlp.MessageNormalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.StopTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.TimeZone;

/**
 * @author Aljoscha Domonell
 */
@SpringBootTest
@ActiveProfiles("dev")
class MessageIT {

    @Autowired
    ReportService reportService;

    @Autowired
    GTFSService gtfsService;

    private void isMatching(String message, ZonedDateTime time, int expectedId) {
        var expected = gtfsService.getStore().getStopTimeForId(expectedId);
        var stopTime = reportService.analyse(message, time).map(Report::getStopTime);
        Assertions.assertTrue(stopTime.isPresent());
        Assertions.assertEquals(expected, stopTime.get());
    }

    @Test
    void testMessage1() {
        var message = "2 männlich gelesen U9 richtung Rathaus Steglitz schloßstr raus";
        var time = ZonedDateTime.of(2022, 2, 21, 17, 39, 0, 0, TimeZone.getDefault().toZoneId());
        var expected = gtfsService.getStore().getStopTimeForId(1362073);
        var coords = new Coordinates(expected.getStop().getLat(), expected.getStop().getLon());


        var report = reportService.analyse(message, time);

        Assertions.assertTrue(report.isPresent());

        Assertions.assertEquals(expected.getStop().getName(), report.get().getTitel());
        Assertions.assertEquals(time, report.get().getTime());
        Assertions.assertEquals(message, report.get().getOriginalMessage());
        Assertions.assertEquals(expected.getTrip().getRoute().getShortName(), report.get().getRoute().getShortName());
        Assertions.assertEquals(expected.getStop().getName(), report.get().getStop().getName());
        Assertions.assertEquals(coords, report.get().getCoordinates());
        Assertions.assertNull(report.get().getStopTime());
        Assertions.assertNull(report.get().getTrip());
    }

    @Test
    void testMessage2() {
        var message = "Ring 41, die steigen gleich in Tempelhof aus\n";
        var time = ZonedDateTime.of(2022, 2, 21, 17, 39, 0, 0, TimeZone.getDefault().toZoneId());
        var stop = "S+U Tempelhof (Berlin)";
        var route = "S41";
        var coords = new Coordinates(52.470694, 13.385754);

        var report = reportService.analyse(message, time);

        Assertions.assertTrue(report.isPresent());

        Assertions.assertEquals(stop, report.get().getTitel());
        Assertions.assertEquals(time, report.get().getTime());
        Assertions.assertEquals(message, report.get().getOriginalMessage());
        Assertions.assertEquals(route, report.get().getRoute().getShortName());
        Assertions.assertEquals(stop, report.get().getStop().getName());
        Assertions.assertEquals(coords, report.get().getCoordinates());
        Assertions.assertNull(report.get().getStopTime());
        Assertions.assertNull(report.get().getTrip());
    }

    @Test
    void testMessage3() {
        var message = "Zoologischer 3 kontrolletis";
        var time = ZonedDateTime.of(2022, 2, 21, 17, 39, 0, 0, TimeZone.getDefault().toZoneId());
        var stop = "S+U Zoologischer Garten Bhf (Berlin)";
        var coords = new Coordinates(52.506921, 13.332707);

        var report = reportService.analyse(message, time);

        Assertions.assertTrue(report.isPresent());

        Assertions.assertEquals(stop, report.get().getTitel());
        Assertions.assertEquals(time, report.get().getTime());
        Assertions.assertEquals(message, report.get().getOriginalMessage());
        Assertions.assertNull(report.get().getRoute());
        Assertions.assertEquals(stop, report.get().getStop().getName());
        Assertions.assertEquals(coords, report.get().getCoordinates());
        Assertions.assertNull(report.get().getStopTime());
        Assertions.assertNull(report.get().getTrip());
    }

    /*
     * TODO
     * M10 Richtung Hauptbahnhof höhe Straßmannstr)//team blauwesten
     * s25 richtung südkreuz jetzt nordbahnhof
     */
//
//    @Test
//    void testMessage4() {
//        String message = "U9 direction rathaus steglitz, now at zoologischere garten";
//        sldMatches("zoologischer garten", "U9", "rathaus steglitz", message);
//    }
//
//    @Test
//    void testMessage5() {
//        String message = "U9 Hansaplatz richtung Osloer 3 mänlich gelesen und 2weiblich mit schwarzen jacken und dunkle Haare";
//        sldMatches("hansaplatz", "U9", "osloer str", message);
//    }
//
//    @Test
//    void testMessage6() {
//        String message = "3 kontrolletis grade am Treptower park ausgestiegen. Kamen aus der Ringbahn Richtung Frankfurter Allee.";
//        sldMatches("treptower park", null, "frankfurter allee", message);
//    }
//
//    @Test
//    void testMessage7() {
//        String message = "Now Karl Marx str they are both in black one has glasses";
//        sldMatches("karlmarxstr", null, null, message);
//    }
//
//    private void sldMatches(String stop, String line, String direction, String message) {
//        Optional<SLD> sld = this.matcher.parse(message);
//        Assertions.assertTrue(sld.isPresent());
//        Assertions.assertEquals(stop, sld.get().getStop().getName());
//        if (line != null) {
//            Assertions.assertEquals(line, sld.get().getLine().getName());
//        } else {
//            Assertions.assertNull(sld.get().getLine());
//        }
//        if (direction != null) {
//            Assertions.assertEquals(direction, sld.get().getDirection().getName());
//        } else {
//            Assertions.assertNull(sld.get().getDirection());
//        }
//    }
}
