package com.aljodomo.kontis.gtfs;

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum RouteType {
    RailwayService(100, 200),
    CoachService(200, 400),
    UrbanRailwayService(400, 700),
    BusService(700, 800),
    TrolleybusService(800, 900),
    TramService(900, 1000),
    UNKNOWN(1000, -1);

    private int lower;
    private int upper;

    public static RouteType fromType(int type) {
        return Arrays.stream(RouteType.values())
                .filter(routeType -> routeType.lower <= type && routeType.upper > type)
                .findFirst()
                .orElse(RouteType.UNKNOWN);
    }
}
