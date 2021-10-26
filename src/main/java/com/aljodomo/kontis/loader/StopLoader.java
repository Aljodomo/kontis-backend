package com.aljodomo.kontis.loader;

import com.aljodomo.kontis.domain.Stop;

import java.util.Map;

/**
 * @author Aljoscha Domonell
 */
public interface StopLoader {
    Map<String, Stop> getStations();
}
