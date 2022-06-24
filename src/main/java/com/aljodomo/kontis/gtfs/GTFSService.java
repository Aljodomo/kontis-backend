package com.aljodomo.kontis.gtfs;

import com.aljodomo.kontis.nlp.MessageNormalizer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides functionalities to work with GTFS data.
 *
 * @author Aljoscha Domonell
 */
@Service
@Slf4j
@Getter
public class GTFSService {

    private final GtfsDaoImpl store = new GtfsDaoImpl();
    private final MessageNormalizer messageNormalizer;

    private final Map<String, Set<Route>> routes = new HashMap<>();
    private final Map<String, Set<Stop>> stops = new HashMap<>();
    private final Map<String, Set<Trip>> trips = new HashMap<>();

    private final Map<Route, Set<String>> routeStops = new HashMap<>();
    private final Map<Route, Set<String>> routeTrips = new HashMap<>();
    private final Map<Stop, Set<StopTime>> stopStopTimes = new HashMap<>();
    private final Map<Trip, Set<StopTime>> tripStopTimes = new HashMap<>();

    private final Set<StopTime> stopTimes = new LinkedHashSet<>();

    private final GTFSFilterProps props;

    @Autowired
    public GTFSService(GTFSFilterProps props, MessageNormalizer messageNormalizer) throws IOException {
        this.props = props;
        this.messageNormalizer = messageNormalizer;

        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(new File("src/main/resources/GTFS"));
        reader.setEntityStore(store);
        reader.run();
        ini();

        logDetails();
    }

    /**
     * {@link GTFSService#getRoutes()} key.
     */
    public String getKey(Route route) {
        return messageNormalizer.normalize(route.getShortName());
    }

    /**
     * {@link GTFSService#getTrips()} key.
     */
    @Nullable
    public String getKey(Trip trip) {
        if (trip.getTripHeadsign() == null) {
            return null;
        }
        String simplifiedName = removeVbbDetails(trip.getTripHeadsign());
        return messageNormalizer.normalize(simplifiedName);
    }

    /**
     * A {@link GTFSService#getStops() stop} can have multiple aliases and therefore multiple keys,
     */
    public List<String> getKeys(Stop stop) {
        String simplifiedName = removeVbbDetails(stop.getName());
        return findAliases(simplifiedName)
                .stream()
                .map(messageNormalizer::normalize)
                .collect(Collectors.toList());
    }

    public List<StopTime> findReachable(StopTime origin, ZonedDateTime time) {

        Set<Stop> originStops = stops.get(getKeys(origin.getStop()).get(0));

        return trips.values().parallelStream()
                .flatMap(Collection::parallelStream)
                .filter(trip -> isActiveAt(trip, LocalDate.from(time)))
                .filter(trip -> tripStopTimes.get(trip).stream().anyMatch(stopTime -> originStops.contains(stopTime.getStop()) && stopHereThen(time, stopTime)))
                .map(trip -> {
                    int sequence = tripStopTimes.get(trip).stream().filter(stopTime -> originStops.contains(stopTime.getStop())).findFirst().map(StopTime::getStopSequence).orElse(0);
                    return tripStopTimes.get(trip).stream()
                            .filter(stopTime -> stopTime.getStopSequence() >= sequence)
                            .filter(stopTime -> stopTime.getArrivalTime() < secondsOfDay(time.plus(10, ChronoUnit.MINUTES)));
                })
                .flatMap(Stream::distinct).collect(Collectors.toList());
    }

    private boolean stopHereThen(ZonedDateTime time, StopTime stopTime) {
        return stopTime.getArrivalTime() > secondsOfDay(time.plus(10, ChronoUnit.MINUTES)) &&
                stopTime.getArrivalTime() < secondsOfDay(time.minus(1, ChronoUnit.MINUTES));
    }

    private long secondsOfDay(ZonedDateTime time){
        Instant midnight = time.toLocalDate().atStartOfDay(time.getZone()).toInstant();
        Duration duration = Duration.between(midnight, Instant.now());
        return duration.getSeconds();
    }

    public List<Stop> getAllStops() {
        return this.stops.values().stream().map(stops1 -> stops1.stream().findFirst().get()).collect(Collectors.toList());
    }

    /**
     * Check if the trip is at the given date.
     */
    public boolean isActiveAt(Trip trip, LocalDate date) {

        ServiceCalendar calendar = store.getAllCalendars().stream()
                .filter(serviceCalendar -> serviceCalendar.getServiceId().equals(trip.getServiceId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active calendar for trip: " + trip));

        switch (date.getDayOfWeek().getValue()) {
            case 1:
                return calendar.getMonday() == 1;
            case 2:
                return calendar.getTuesday() == 1;
            case 3:
                return calendar.getWednesday() == 1;
            case 4:
                return calendar.getThursday() == 1;
            case 5:
                return calendar.getFriday() == 1;
            case 6:
                return calendar.getSaturday() == 1;
            case 7:
                return calendar.getSunday() == 1;
            default:
                throw new IllegalStateException("WTF is wrong with LocalDateTime?! Or am I the problem...?");
        }
    }

    /**
     * Find the stopTime which is closest in time to the given time and belongs to the trip and is at the specified stop.
     */
    public Optional<StopTime> findStopTime(Trip trip, Stop stop, ZonedDateTime time) {
        return stopTimes
                .stream()
                .filter(stopTime -> stopTime.getTrip().equals(trip))
                .filter(stopTime -> stopTime.getStop().equals(stop))
                .min((o1, o2) -> compareToNow(time, o1, o2));
    }

    /**
     * Find the stopTime which is closest in time to the given time and belongs to the trip and is one of the stopCandidates.
     */
    public Optional<StopTime> findStopTime(Trip trip, List<Stop> stopCandidates, ZonedDateTime time) {
        return stopTimes
                .stream()
                .filter(stopTime -> stopTime.getTrip().equals(trip))
                .filter(stopTime -> stopCandidates.contains(stopTime.getStop()))
                .min((o1, o2) -> compareToNow(time, o1, o2));
    }

    /**
     * Find all possible StopTimes for the given route, stops and time.
     *
     * @param routes         Routes to search in.
     * @param stopCandidates Stops to filter by.
     * @param time           Time to compare to the stopsTimes time.
     * @return Possibles stopTimes.
     */
    public List<StopTime> findStopTimes(List<Route> routes, List<Stop> stopCandidates, ZonedDateTime time) {
        // True is one direction and false the other
        Map<Boolean, List<StopTime>> directionalStopTimes = this.stopTimes.stream()
                .filter(stopTime -> routes.contains(stopTime.getTrip().getRoute()))
                .filter(stopTime -> stopCandidates.contains(stopTime.getStop()))
                .filter(stopTime -> isActiveAt(stopTime.getTrip(), LocalDate.from(time)))
                .collect(Collectors.partitioningBy(o -> o.getTrip().getDirectionId().equals("1")));

        List<StopTime> candidates = new ArrayList<>();

        directionalStopTimes.get(true)
                .stream().min((o1, o2) -> compareToNow(time, o1, o2))
                .ifPresent(candidates::add);

        directionalStopTimes.get(false).stream()
                .min((o1, o2) -> compareToNow(time, o1, o2))
                .ifPresent(candidates::add);

        return candidates;
    }

    /**
     * Get a list of all stops of the given route.
     */
    public List<String> findStopNames(List<Route> routes) {
        return routes.stream()
                .map(routeStops::get)
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all displayed head signs of the given routes.
     * There are at least two head signs per route.
     * One for each direction.
     */
    public List<String> findHeadSigns(List<Route> routes) {
        return routes.stream()
                .map(routeTrips::get)
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    /**
     * Check if the stop is part of the given route.
     */
    public boolean isPartOf(Route route, Stop stop) {
        return routeStops.get(route).stream().anyMatch(s -> stops.get(s).contains(stop));
    }

    private int getNormalizedSecondOfDay(StopTime o1) {
        return o1.getArrivalTime() % 86399;
    }

    private int compareToNow(ZonedDateTime now, StopTime o1, StopTime o2) {
        // TODO arrival times are sometimes more then 24 houres. in this case the next day started
        LocalTime time = LocalTime.ofSecondOfDay(getNormalizedSecondOfDay(o1));
        LocalTime time2 = LocalTime.ofSecondOfDay(getNormalizedSecondOfDay(o2));
        return Math.abs(now.toLocalTime().toSecondOfDay() - time.toSecondOfDay()) -
                Math.abs(now.toLocalTime().toSecondOfDay() - time2.toSecondOfDay());
    }

    /**
     * Filter routes while loading
     */
    private boolean shouldBeLoaded(Route route) {
        return props.getAgencyWhitelist().contains(route.getId().getAgencyId())
                && route.getShortName().matches(props.getRouteShortNameRegEx());
    }

    private void ini() {
        log.debug("Start loading route");
        Set<Route> tmpRouteSet = iniRoutes();

        log.debug("Start loading trips");
        Set<Trip> tmpTripSet = iniTrips(tmpRouteSet);

        log.debug("Start loading stopTimes");
        iniStopTimes(tmpTripSet);

        log.debug("Start loading stops");
        iniStops();
    }

    private Set<Route> iniRoutes() {
        Set<Route> tmpRouteSet = new HashSet<>();

        store.getAllRoutes()
                .stream()
                .filter(this::shouldBeLoaded)
                .forEach(route -> {
                    String name = getKey(route);
                    add(routes, name, route);

                    tmpRouteSet.add(route);
                });
        return tmpRouteSet;
    }

    private Set<Trip> iniTrips(Set<Route> tmpRouteSet) {
        Set<Trip> tmpTripSet = new HashSet<>();

        store.getAllTrips()
                .stream()
                .filter(trip -> tmpRouteSet.contains(trip.getRoute()))
                .forEach(trip -> {
                    if (trip.getTripHeadsign() == null) {
                        return;
                    }
                    String headSign = getKey(trip);
                    add(trips, headSign, trip);

                    add(routeTrips, trip.getRoute(), headSign);

                    tmpTripSet.add(trip);
                });
        return tmpTripSet;
    }

    private void iniStopTimes(Set<Trip> tmpTripSet) {
        store.getAllStopTimes().stream()
                .filter(stopTime -> tmpTripSet.contains(stopTime.getTrip()))
                .forEach(stopTimes::add);
    }

    private void iniStops() {
        stopTimes.forEach(stopTime -> {
            getKeys(stopTime.getStop())
                    .stream()
                    .map(messageNormalizer::normalize)
                    .forEach(alias -> {
                        Stop stop = stopTime.getStop();
                        add(stops, alias, stop);

                        add(stopStopTimes, stop, stopTime);

                        add(routeStops, stopTime.getTrip().getRoute(), alias);

                        add(tripStopTimes, stopTime.getTrip(), stopTime);
                    });
        });
    }

    private void logDetails() {
        int rCnt = routes.values().stream().map(Set::size).reduce(Integer::sum).orElse(0);
        int rCntO = store.getAllRoutes().size();

        int tCnt = trips.values().stream().map(Set::size).reduce(Integer::sum).orElse(0);
        int tCntO = store.getAllTrips().size();

        int sCnt = stops.values().stream().map(Set::size).reduce(Integer::sum).orElse(0);
        int sCntO = store.getAllStops().size();

        int stCnt = stopTimes.size();
        int stCntO = store.getAllStopTimes().size();

        log.info("Successfully loaded {} of {} routes, {} of {} trips, {} of {} stops and {} of {} stopTimes",
                rCnt, rCntO, tCnt, tCntO, sCnt, sCntO, stCnt, stCntO);
    }

    private <K, V> void add(Map<K, Set<V>> map, K key, V value) {
        if (map.get(key) != null) {
            map.get(key).add(value);
        } else {
            // Ini bucket
            Set<V> entry = new LinkedHashSet<>();
            entry.add(value);
            map.put(key, entry);
        }
    }

    private List<String> findAliases(String stopName) {
        if (stopName.contains("/")) {
            return List.of(stopName.split("/"));
        } else {
            return Collections.singletonList(stopName);
        }
    }

    private String removeVbbDetails(String originalStopName) {
        return originalStopName
                .replaceAll("(S )|(S\\+U )|(U )", "")
                .replaceAll("\\(.*\\).*", "")
                .replaceAll("\\[.*\\].*", "")
                .replace("Bhf", "");
    }

}
