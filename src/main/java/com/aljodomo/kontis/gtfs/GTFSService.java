package com.aljodomo.kontis.gtfs;

import com.aljodomo.kontis.nlp.MessageNormalizer;
import com.aljodomo.kontis.nlp.Precision;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.ricecode.similarity.SimilarityScore;
import net.ricecode.similarity.StringSimilarityService;
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
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.function.Function;
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
    private final StringSimilarityService similarityService;

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
    public GTFSService(GTFSFilterProps props, MessageNormalizer messageNormalizer, StringSimilarityService similarityService) throws IOException {
        this.props = props;
        this.messageNormalizer = messageNormalizer;
        this.similarityService = similarityService;

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

    /**
     * Find all future stoptimes that can be reached by swichting the route at max ones and following it for a giving time.
     *
     * @param origin source from which extrapolate
     * @param time of sighing
     * @param lookAhead time in the future to take into account
     * @return
     */
    public List<StopTime> findReachable(StopTime origin, ZonedDateTime time, TemporalAmount lookAhead) {

        Set<Stop> originStops = stops.get(getKeys(origin.getStop()).get(0));

        Stream<Trip> reachableTrips = trips.values().parallelStream()
                .flatMap(Collection::parallelStream)
                .filter(trip -> isActiveAt(trip, LocalDate.from(time)))
                .filter(trip -> tripStopTimes.get(trip).stream()
                        .anyMatch(stopTime -> originStops.contains(stopTime.getStop()) && isCloseToGivenTime(stopTime, time, lookAhead)));

        Stream<Stream<StopTime>> reachableStopTimes = reachableTrips
                .map(trip -> {
                    int sequence = getSequence(originStops, trip);
                    return tripStopTimes.get(trip).stream()
                            .filter(stopTime -> stopTime.getStopSequence() >= sequence)
                            .filter(stopTime -> arrivesBefore(stopTime, time.plus(lookAhead)));
                });

        return reachableStopTimes.flatMap(Stream::distinct).collect(Collectors.toList());
    }

    private boolean arrivesBefore(StopTime stopTime, ZonedDateTime time) {
        return toLDT(time.toLocalDate(), stopTime.getArrivalTime()).isBefore(time.toLocalDateTime());
    }

    private Integer getSequence(Set<Stop> originStops, Trip trip) {
        return tripStopTimes.get(trip).stream()
                .filter(stopTime -> originStops.contains(stopTime.getStop()))
                .findFirst()
                .map(StopTime::getStopSequence)
                .orElse(0);
    }

    private boolean isCloseToGivenTime(StopTime stopTime, ZonedDateTime time, TemporalAmount lookAhead) {
        LocalDateTime arrivalTime = toLDT(time.toLocalDate(), stopTime.getArrivalTime());

        LocalDateTime max = time.toLocalDateTime().plus(lookAhead);
        LocalDateTime min = time.toLocalDateTime().minus(1, ChronoUnit.MINUTES);

        return arrivalTime.isAfter(min) && arrivalTime.isBefore(max);
    }

    private LocalDateTime toLDT(LocalDate time, int gtfsSecondsOfDay){
        return time.atStartOfDay().plus(gtfsSecondsOfDay, ChronoUnit.SECONDS);
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

    public Optional<StopTime> findStopTime(ZonedDateTime time, List<Route> routes, String direction, List<Stop> stops) {
        List<StopTime> stopTimes = findStopTimes(routes, stops, time);

        return identifyDirection(stopTimes, direction);
    }

    private Optional<StopTime> identifyDirection(List<StopTime> stopTimes, String direction) {
        if(stopTimes.size() <= 1) {
            return stopTimes.stream().findFirst();
        }

        if (stopTimes.size() > 2) {
            log.warn("More then two stopTimes were supplied [{}]", join(stopTimes, st -> st.getId().toString()));
        }

        Map<String, StopTime> map = stopTimes.stream()
                .collect(Collectors.toMap(stopTime ->
                        messageNormalizer.normalize(stopTime.getTrip().getTripHeadsign()), Function.identity()));

        if (map.size() != stopTimes.size()) {
            log.warn("StopTimes were lost while grouping them by their normalized trip head sign [{}] != [{}]",
                    join(stopTimes, st -> st.getId().toString()),
                    join(map.values(), st -> st.getId().toString()));
        }

        SimilarityScore similarityScore = similarityService
                .findTop(List.of(map.keySet().toArray(new String[0])), direction);

        if (similarityScore.getScore() < Precision.HIGH) {
            log.warn("Highest similarity score from [{}] to any known stop is lower then precision threshold [{} < {}]",
                    similarityScore.getKey(), similarityScore.getScore(), Precision.HIGH);
        }

        return Optional.ofNullable(map.get(similarityScore.getKey()));
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
                && route.getShortName().matches(props.getRouteShortNameRegEx())
                && isTrain(route);
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

    private boolean isTrain(Route route) {
        int type = route.getType();

        RouteType routeType = RouteType.fromType(type);

        return  routeType == RouteType.RailwayService ||
                routeType == RouteType.TramService ||
                routeType == RouteType.UrbanRailwayService;
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

    private <T> String join(Collection<T> objs, Function<T, String> keyMapper) {
        return objs.stream().map(keyMapper).collect(Collectors.joining(","));
    }

}
