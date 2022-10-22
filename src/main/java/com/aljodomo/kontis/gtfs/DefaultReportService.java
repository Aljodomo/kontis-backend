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

        // 3. Identify Direction
        Optional<String> direction;
        // Direction is removed from messageWords
        if (!routes.isEmpty()) {
            direction = findDirection(messageWords, routes);
        } else {
            direction = findDirection(messageWords);
        }

        direction.ifPresent(s -> log.debug("Identified direction [{}]", s));

        // 3. Identify Stop
        List<Stop> stops = getStops(messageWords, routes);
        log.debug("Identified stops [{}]", joinDistinct(stops, Stop::getName));

        if (!routes.isEmpty()) {
            routes = filterRoutes(routes, stops);
            log.debug("Filtered routes [{}]", joinDistinct(routes, Route::getShortName));
        }

        return getReport(message, time, routes, direction.orElse(null), stops);
    }

    private Optional<Report> getReport(String message, ZonedDateTime time, List<Route> routes, String direction, List<Stop> stops) {

        // TODO falls keine direction angeben wurde, kann hier einfach eine "zufällige" richtung genommen werden. Die Methode gibt es schon im GTFSService.
        if (!routes.isEmpty() && !stops.isEmpty() && direction != null) {
            // 4. Identify StopTime
            Optional<StopTime> stopTimeOp = gtfsService.findStopTime(time, routes, direction, stops);
            if (stopTimeOp.isPresent()) {
                log.info("Building complete report. StopTimeId[{}] Route[{}] Stop[{}]",
                        stopTimeOp.get().getId(),
                        stopTimeOp.get().getTrip().getRoute().getShortName(),
                        stopTimeOp.get().getStop().getName()
                );
                return Optional.of(new Report(message, time, stopTimeOp.get()));
            }
        }

        Optional<Stop> distinctStop = findDistinctParentStop(stops);
        Optional<String> distinctRouteName = findDistinctRouteName(routes);

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
                    someStop.getName()
            );
            return Optional.of(new Report(message, time, someStop));
        }


        log.info("Not enough information to build report");
        return Optional.empty();
    }

    private Optional<Stop> findDistinctParentStop(List<Stop> stops) {
        List<Stop> distinctStops = stops.stream()
                .map(gtfsService::getParentStation)
                .distinct()
                .collect(Collectors.toList());

        if (distinctStops.size() != 1) {
            log.warn("More then one unique stop name");
            return Optional.empty();
        } else {
            return Optional.of(distinctStops.get(0));
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

    private List<Stop> getStops(List<String> messageWords, List<Route> routes) {
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

    private Optional<String> findDirection(List<String> messageWords) {
        List<String> headSigns = new ArrayList<>(gtfsService.getTrips().keySet());
        return getDirection(messageWords, headSigns);
    }

    private Optional<String> findDirection(List<String> messageWords, List<Route> routes) {
        List<String> headSigns = gtfsService.findHeadSigns(routes);
        return getDirection(messageWords, headSigns);
    }

    private Optional<String> getDirection(List<String> messageWords, List<String> headSigns) {
        Optional<String> direction;
        List<String> directions = directionRemover.cutHeadSignsAndKeywords(messageWords, headSigns);
        direction = Optional.ofNullable(findFirst(directions));
        return direction;
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

    private String findFirst(List<String> directions) {
        if (directions.isEmpty()) {
            log.debug("No direction was identified");
            return null;
        } else {
            if (directions.size() > 1) {
                log.info("More then one direction was identified but only the first one will be used [{}]", directions);
            }

            return directions.get(0);
        }
    }

    private List<Route> parseRoute(List<String> words) {

        List<Route> routes = words.stream()
                .filter(word -> gtfsService.getRoutes().containsKey(word))
                .map(s -> gtfsService.getRoutes().get(s))
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        if (routes.isEmpty()) {

            if (words.contains("41")) {
                routes.addAll(gtfsService.getRoutes().get("s41"));
            } else if (words.contains("42")) {
                routes.addAll(gtfsService.getRoutes().get("s42"));
            } else if (words.contains("ring")) {
                routes.addAll(gtfsService.getRoutes().get("s41"));
                routes.addAll(gtfsService.getRoutes().get("s42"));
            } else if (words.contains("s")) {
                gtfsService.getRoutes()
                        .keySet().stream()
                        .filter(routeKey -> routeKey.startsWith("s"))
                        .forEach(routeKey -> routes.addAll(gtfsService.getRoutes().get(routeKey)));
            } else if (words.contains("u")) {
                gtfsService.getRoutes()
                        .keySet().stream()
                        .filter(routeKey -> routeKey.startsWith("u"))
                        .forEach(routeKey -> routes.addAll(gtfsService.getRoutes().get(routeKey)));
            }
        }

        return routes;
    }
}
