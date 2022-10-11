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
        // Needs to be an ArrayList to allow List.sublist().clear() to work.
        ArrayList<String> messageWords = new ArrayList<>(List.of(normalizedMessage.split(" ")));
        log.debug("Normalized message [{}]", normalizedMessage);

        // 2. Identify Route
        List<Route> routes = parseRoute(messageWords);
        log.debug("Identified routes [{}]", join(routes, Route::getShortName));

        // 3. Identify Direction
        Optional<String> direction = Optional.empty();
        if (!routes.isEmpty()) {
            direction = getDirection(messageWords, routes);
            log.debug("Identified direction [{}]", direction.orElse(null));
        }

        // 3. Identify Stop
        List<Stop> stops = getStops(messageWords, routes);
        log.debug("Identified stops [{}]", join(stops, Stop::getName));

        routes = filterRoutes(routes, stops);
        log.debug("Filtered routes [{}]", join(routes, Route::getShortName));

        return getReport(message, time, routes, direction, stops);
    }

    private Optional<Report> getReport(String message, ZonedDateTime time, List<Route> routes, Optional<String> direction, List<Stop> stops) {

        // TODO falls keine direction angeben wurde, kann hier einfach eine "zufällige" richtung genommen werden. Die Methode gibt es schon im GTFSService.
        if (!routes.isEmpty() && !stops.isEmpty() && direction.isPresent()) {
            // 4. Identify StopTime
            Optional<StopTime> stopTimeOp = gtfsService.findStopTime(time, routes, direction.get(), stops);
            if (stopTimeOp.isPresent()) {
                return buildReport(message, time, stopTimeOp.get());
            }
        }

        if (!routes.isEmpty() && !stops.isEmpty()) {
            Stop someStop = stops.get(0);
            Route someRoute = routes.stream()
                    .filter(route -> gtfsService.isPartOf(route, someStop))
                    .findAny()
                    .orElse(null);
            return buildReport(message, time, someStop, someRoute);
        }

        if(!stops.isEmpty()){
            Stop someStop = stops.get(0);
            return buildReport(message, time, someStop, null);
        }

        if (!routes.isEmpty()) {
            // TODO handle route only
        }


        log.info("Not enough information to build report");
        return Optional.empty();
    }

    private List<Route> filterRoutes(List<Route> routes, List<Stop> stops) {
        return routes.stream()
                .filter(route -> stops.stream()
                        .anyMatch(stop -> gtfsService.isPartOf(route, stop)))
                .collect(Collectors.toList());
    }

    private List<Stop> getStops(ArrayList<String> messageWords, List<Route> routes) {
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

    private Optional<String> getDirection(ArrayList<String> messageWords, List<Route> routes) {
        Optional<String> direction;
        List<String> headSigns = gtfsService.findHeadSigns(routes);
        List<String> directions = directionRemover.removeDirections(messageWords, headSigns);
        direction = Optional.ofNullable(getFirstDirection(directions));
        return direction;
    }

    private Optional<Report> buildReport(String message, ZonedDateTime time, Stop someStop, Route someRoute) {
        Coordinates coords = new Coordinates(someStop.getLat(), someStop.getLon());
        return Optional.of(Report.builder()
                .title(someStop.getName())
                .originalMessage(message)
                .time(time)
                .coordinates(coords)
                .stop(someStop)
                .route(someRoute)
                .build());
    }

    private Optional<Report> buildReport(String message, ZonedDateTime time, StopTime res) {
        Coordinates coords = new Coordinates(res.getStop().getLat(), res.getStop().getLon());

        return Optional.of(Report.builder()
                .title(stopTime.getStop().getName())
                .originalMessage(message)
                .stopTimeId(stopTime.getId())
                .time(time)
                .coordinates(coords)
                .stopTime(res)
                .stop(res.getStop())
                .route(res.getTrip().getRoute())
                .trip(res.getTrip())
                .build());
    }


    private <T> String join(Collection<T> objs, Function<T, String> keyMapper) {
        return objs.stream().map(keyMapper).collect(Collectors.joining(","));
    }

    private List<Stop> parseStops(String cleanedMessage, List<String> stopNames) {
        List<SimilarityScore> scores = similarityService.scoreAll(stopNames, cleanedMessage);

        return scores.stream()
                .filter(similarityScore -> similarityScore.getScore() > Precision.HIGH)
                .map(similarityScore -> gtfsService.getStops().get(similarityScore.getKey()))
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    private String getFirstDirection(List<String> directions) {
        if (directions.isEmpty()) {
            log.debug("No directions were identified [{}]", directions);
            return null;
        } else {
            if (directions.size() > 1) {
                log.info("More then one direction was identified but only the first one will be used [{}]", directions);
            }

            return directions.get(0);
        }
    }

    private List<Route> parseRoute(ArrayList<String> words) {

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
