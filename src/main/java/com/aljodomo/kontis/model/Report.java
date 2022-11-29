package com.aljodomo.kontis.model;

import lombok.Data;
import org.onebusaway.gtfs.model.StopTime;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.UUID;


/**
 * @author Aljoscha Domonell
 */
@Data
public class Report {

    final UUID id = UUID.randomUUID();

    final String title;
    final String originalMessage;
    final Coordinates coordinates;
    final ZonedDateTime time;
    @Nullable
    final String routeId;
    @Nullable
    final String routeName;
    @Nullable
    final String stopId;
    final String stopName;
    @Nullable
    final String tripId;
    @Nullable
    final Integer stopTimeId;

    public Report(String massage, ZonedDateTime time, StopTime stopTime) {
        this.title = stopTime.getTrip().getRoute().getShortName() + " " + stopTime.getStop().getName();
        this.originalMessage = massage;
        this.coordinates = new Coordinates(stopTime.getStop().getLat(), stopTime.getStop().getLon());
        this.stopTimeId = stopTime.getId();
        this.time = time;
        this.routeId = stopTime.getTrip().getRoute().getId().toString();
        this.routeName = stopTime.getTrip().getRoute().getShortName();
        this.stopId = stopTime.getStop().getId().toString();
        this.stopName = stopTime.getStop().getName();
        this.tripId = stopTime.getTrip().getRoute().getId().toString();
    }

    public Report(String message, ZonedDateTime time, Coordinates coordinates, String stopName, String routeName) {
        this.title = routeName + " " + stopName;
        this.originalMessage = message;
        this.coordinates = coordinates;
        this.time = time;
        this.routeName = routeName;
        this.stopId = null;
        this.stopName = stopName;
        this.routeId = null;
        this.tripId = null;
        this.stopTimeId = null;
    }

    public Report(String message, ZonedDateTime time, Coordinates coords, String stopName) {
        this.title = stopName;
        this.originalMessage = message;
        this.coordinates = coords;
        this.time = time;
        this.stopId = null;
        this.stopName = stopName;
        this.tripId = null;
        this.routeName = null;
        this.routeId = null;
        this.stopTimeId = null;
    }
}
