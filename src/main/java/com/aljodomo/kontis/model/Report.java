package com.aljodomo.kontis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.firebase.database.core.Repo;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

import java.time.ZonedDateTime;
import java.util.UUID;


/**
 * @author Aljoscha Domonell
 */
@Builder
@Data
@Jacksonized
public class Report {

    @Builder.Default
    UUID id = UUID.randomUUID();
    String titel;
    String originalMessage;
    Coordinates coordinates;
    ZonedDateTime time;
    @JsonIgnore
    StopTime stopTime;
    @JsonIgnore
    Stop stop;
    @JsonIgnore
    Route route;
    @JsonIgnore
    Trip trip;

    public static Report build(String massage, StopTime stopTime, ZonedDateTime time) {
        return Report.builder()
                .titel(stopTime.getStop().getName())
                .originalMessage(massage)
                .coordinates(new Coordinates(stopTime.getStop().getLat(), stopTime.getStop().getLon()))
                .time(time)
                .stopTime(stopTime)
                .stop(stopTime.getStop())
                .route(stopTime.getTrip().getRoute())
                .trip(stopTime.getTrip())
                .build();
    }

}
