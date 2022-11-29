package com.aljodomo.kontis.gtfs;

import com.aljodomo.kontis.model.Coordinates;
import com.aljodomo.kontis.model.Report;
import com.aljodomo.kontis.nlp.DirectionRemover;
import com.aljodomo.kontis.nlp.MessageNormalizer;
import com.aljodomo.kontis.nlp.Precision;
import lombok.extern.slf4j.Slf4j;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.StringSimilarityService;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.aljodomo.kontis.utils.StringUtils.concat;

/**
 * @author Aljoscha Domonell
 */
@Service
@Slf4j
public class DefaultReportService implements ReportService {

    private final MessageNormalizer messageNormalizer;
    private final DirectionRemover directionRemover;
    private final GTFSService gtfsService;
    private final StringSimilarityService similarityService;
    private final Map<String, String> routeSynonyms = Map.of(
            "41", "s41",
            "42", "s42"
    );
    private final Map<String, List<String>> ambiguousRouteSynonyms = Map.of(
            "ring", List.of("s41", "s42"),
            "ringbahn", List.of("s41", "s42")
    );

    @Autowired
    public DefaultReportService(MessageNormalizer messageNormalizer, DirectionRemover directionRemover, GTFSService gtfsService, StringSimilarityService similarityService) {
        this.messageNormalizer = messageNormalizer;
        this.directionRemover = directionRemover;
        this.gtfsService = gtfsService;
        this.similarityService = similarityService;
    }

    @Override
    public Optional<Report> analyse(String message, ZonedDateTime time) {

        log.debug("Message [{}]", message);

        if (message.contains("?")) {
            log.info("Message [{}] seems to be a question and will not be considered as a report", message);
            return Optional.empty();
        }

        // 1. Preparation
        String normalizedMessage = messageNormalizer.normalize(message);
        log.debug("Normalized message [{}]", normalizedMessage);
        List<String> messageWords = new ArrayList<>(List.of(normalizedMessage.split(" ")));

        // 2. Identify Route
        List<Route> routes = parseRoute(messageWords);
        log.debug("Identified routes [{}]", joinDistinct(routes, Route::getShortName));

        // 3. Identify direction
        Optional<String> direction = parseAndCutDirection(messageWords, routes);
        direction.ifPresent(s -> log.debug("Identified direction [{}]", s));

        // 4. Identify Stops - Must be done AFTER direction is removed from message
        List<Stop> stops = parseStops(messageWords, routes);
        log.debug("Identified stops [{}]", joinDistinct(stops, Stop::getName));

        // 5. Filter routes with identified stops
        routes = filterRoutes(routes, stops);
        log.debug("Filtered routes [{}]", joinDistinct(routes, Route::getShortName));

        // 6. Identify stopTime
        Optional<StopTime> stopTimeOp = Optional.empty();
        if (isCircleRoute(routes)) {
            // 6.1 Handle circle route
            stopTimeOp = gtfsService.findStopTimes(routes, stops, time).stream().findFirst();
        } else {
            // 6.2 Handle normal route
            if (direction.isPresent()) {
                stopTimeOp = gtfsService.findStopTime(time, routes, direction.get(), stops);
            }
        }

        // 7. Build report
        if(stopTimeOp.isPresent()) {
            return buildCompleteReport(message, time, stopTimeOp.get());
        }
        return buildPartialReport(message, time, routes, stops);
    }

    private static Optional<Report> buildCompleteReport(String message, ZonedDateTime time, StopTime stopTime) {
        log.info("Building complete report. StopTimeId[{}] Route[{}] Stop[{}]",
                stopTime.getId(),
                stopTime.getTrip().getRoute().getShortName(),
                stopTime.getStop().getName()
        );
        return Optional.of(new Report(message, time, stopTime));
    }

    private boolean isCircleRoute(List<Route> routes) {
        return  routes.stream().map(Route::getShortName).distinct().count() == 1 // Is only one route
                && routes.stream().allMatch(route -> isCircleRoute(route.getShortName()));
    }

    private boolean isCircleRoute(String shortName) {
        return shortName.equals("S41") || shortName.equals("S42");
    }

    private Optional<Report> buildPartialReport(String message, ZonedDateTime time, List<Route> routes, List<Stop> stops) {

        Optional<Stop> distinctStop = findFirstIfAllNamesAreEqual(stops); // TODO rename, logs
        Optional<String> distinctRouteName = findDistinctRouteName(routes); // TODO rename, logs

        if (distinctStop.isPresent() && distinctRouteName.isPresent()) {
            Stop someStop = distinctStop.get();
            log.info("Building partial report. StopTimeId[] Route[{}] Stop[{}]",
                    distinctRouteName.get(),
                    someStop.getName()
            );
            Coordinates coords = new Coordinates(someStop.getLat(), someStop.getLon());
            return Optional.of(new Report(message, time, coords, someStop.getName(), distinctRouteName.get()));
        }

        if (distinctStop.isPresent()) {
            Stop someStop = distinctStop.get();
            log.info("Building partial report. StopTimeId[] Route[] Stop[{}]",
                    someStop.getName());
            Coordinates coords = new Coordinates(someStop.getLat(), someStop.getLon());
            return Optional.of(new Report(message, time, coords, someStop.getName()));
        }


        log.info("Not enough information to build report");
        return Optional.empty();
    }

    private Optional<Stop> findFirstIfAllNamesAreEqual(List<Stop> stops) {
        List<String> distinctStops = stops.stream()
                .map(Stop::getName)
                .distinct()
                .collect(Collectors.toList());

        if (distinctStops.size() != 1) {
            log.warn("Stops are not distinct");
            return Optional.empty();
        } else {
            log.info("Stop names are all the same. Using the first stop");
            return Optional.of(stops.get(0));
        }
    }

    private Optional<String> findDistinctRouteName(List<Route> routes) {
        List<String> distinctRouteNames = routes.stream()
                .map(Route::getShortName)
                .distinct()
                .collect(Collectors.toList());

        if (distinctRouteNames.size() != 1) {
            log.warn("More then one unique route name");
            return Optional.empty();
        } else {
            return Optional.of(distinctRouteNames.get(0));
        }
    }

    private List<Route> filterRoutes(List<Route> routes, List<Stop> stops) {
        return routes.stream()
                .filter(route -> stops.stream()
                        .anyMatch(stop -> gtfsService.isPartOf(route, stop)))
                .collect(Collectors.toList());
    }

    private List<Stop> parseStops(List<String> messageWords, List<Route> routes) {
        String cleanedMessage = concat(messageWords);
        List<Stop> stops;
        List<String> stopNames;
        if (!routes.isEmpty()) {
            stopNames = gtfsService.findStopNames(routes);
        } else {
            stopNames = new ArrayList<>(gtfsService.getStops().keySet());
        }
        stops = parseStops(cleanedMessage, stopNames);
        return stops;
    }

    private Optional<String> parseAndCutDirection(List<String> messageWords, List<Route> routes) {
        List<String> possibleDirections;
        Optional<String> direction;

        // Route headsigns
        possibleDirections = gtfsService.findHeadSigns(routes);
        direction = cutDirection(messageWords, possibleDirections);

        // All headsigns
        if(direction.isEmpty()) {
            log.debug("Using head signs of all available trips as possible directions");
            possibleDirections = new ArrayList<>(gtfsService.getTrips().keySet());
            direction = cutDirection(messageWords, possibleDirections);
        }

        // All Route Stops
        if(direction.isEmpty()) {
            log.debug("Using names of of all stops of found routes as possible directions");
            possibleDirections = gtfsService.findStopNames(routes);
            direction = cutDirection(messageWords, possibleDirections);
        }

        return direction;
    }

    private Optional<String> cutDirection(List<String> messageWords, List<String> stopNames) {
        List<String> directions = directionRemover.cutStopNameWithKeyword(messageWords, stopNames);
        if (directions.size() > 1) {
            log.info("More then one direction was identified but only the first one will be used [{}]", directions);
        }
        return directions.stream().findFirst();
    }

    private <T> String joinDistinct(Collection<T> objs, Function<T, String> keyMapper) {
        return objs.stream().map(keyMapper).distinct().collect(Collectors.joining(","));
    }

    private List<Stop> parseStops(String cleanedMessage, List<String> stopNames) {
        List<SimilarityScore> scores = similarityService.scoreAll(stopNames, cleanedMessage);

        return scores.stream()
                .filter(similarityScore -> similarityScore.getScore() > Precision.HIGH)
                .map(similarityScore -> gtfsService.getStops().get(similarityScore.getKey()))
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    private List<Route> parseRoute(List<String> words) {

        List<Route> routes = words.stream()
                .filter(word -> gtfsService.getRoutes().containsKey(word))
                .map(s -> gtfsService.getRoutes().get(s))
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        if (routes.isEmpty()) {
            parseRouteSynonyms(words, routes);
        }

        if (routes.isEmpty()) {
            parseAmbiguousRouteLetters(words, routes);
        }

        return routes;
    }

    private void parseAmbiguousRouteLetters(List<String> words, List<Route> routes) {
        if (words.contains("s")) {
            log.info("Message did not contain a known route name but a single 's'. Using all S-Bahn routes");
            gtfsService.getRoutes()
                    .keySet().stream()
                    .filter(routeKey -> routeKey.startsWith("s"))
                    .forEach(routeKey -> routes.addAll(gtfsService.getRoutes().get(routeKey)));
        }

        if (words.contains("u")) {
            log.info("Message did not contain a known route name but a single 'u'. Using all U-Bahn routes.");
            gtfsService.getRoutes()
                    .keySet().stream()
                    .filter(routeKey -> routeKey.startsWith("u"))
                    .forEach(routeKey -> routes.addAll(gtfsService.getRoutes().get(routeKey)));
        }
    }


    private void parseRouteSynonyms(List<String> words, List<Route> routes) {

        for (String word : words) {
            if (routeSynonyms.containsKey(word)) {
                log.info("Identified known synonym [{}]. Using [{}] routes", word, ambiguousRouteSynonyms.get(word));
                routes.addAll(gtfsService.getRoutes().get(routeSynonyms.get(word)));
            }
        }

        if(!routes.isEmpty()) {
            return; // Prevent cases like "ring 41" to be counted twice
        }

        for (String word : words) {
            if (ambiguousRouteSynonyms.containsKey(word)) {
                log.info("Identified known synonym [{}]. Using [{}] routes", word, ambiguousRouteSynonyms.get(word));
                ambiguousRouteSynonyms.get(word)
                        .forEach(synonym -> routes.addAll(gtfsService.getRoutes().get(synonym)));
            }
        }
    }
}
