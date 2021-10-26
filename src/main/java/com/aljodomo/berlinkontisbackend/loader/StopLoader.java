package com.aljodomo.berlinkontisbackend.loader;

import com.aljodomo.berlinkontisbackend.domain.Stop;

import java.util.Map;

/**
 * @author Aljoscha Domonell
 */
public interface StopLoader {
    Map<String, Stop> getStations();
}
