package com.aljodomo.kontis.loader;

import com.aljodomo.kontis.domain.Coordinates;
import com.aljodomo.kontis.domain.Stop;
import com.aljodomo.kontis.nlp.preperation.MessageNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Aljoscha Domonell
 */
@Slf4j
@Component
public class VbbFileStopLoader implements StopLoader {

    private final CSVFormat format;

    private final Map<String, Stop> stops = new HashMap<>();
    private final MessageNormalizer messageNormalizer;

    public VbbFileStopLoader(MessageNormalizer messageNormalizer) throws IOException {
        this.messageNormalizer = messageNormalizer;
        this.format = CSVFormat
                .Builder
                .create(CSVFormat.DEFAULT)
                .setHeader("stop_id",
                        "stop_code",
                        "stop_name",
                        "stop_desc",
                        "stop_lat",
                        "stop_lon",
                        "location_type",
                        "parent_station",
                        "wheelchair_boarding",
                        "platform_code",
                        "zone_id")
                .build();
        this.load("src/main/resources/vbb/stops.txt");
    }

    private void load(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {
            format.parse(reader)
                    .getRecords()
                    .forEach(entry -> {
                                if(!isEmpty(entry.get("parent_station"))) {
                                    // Only include the primary station
                                    return;
                                }
                                String stopName = entry.get("stop_name");
                                if (isStation(stopName) && !this.stops.containsKey(stopName)) {
                                    String simplifiedName = removeVbbDetails(stopName);
                                    checkForAliasAndAdd(entry, simplifiedName);
                                }
                            }
                    );
            log.info("Stops loaded: " + this.stops.size());
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().equals("");
    }

    private void checkForAliasAndAdd(CSVRecord entry, String stopName) {
        if (stopName.contains("/")) {
            String[] aliases = stopName.split("/");
            for (String alias : aliases) {
                addStop(entry, alias);
            }
        } else {
            addStop(entry, stopName);
        }
    }

    private void addStop(CSVRecord entry, String alias) {
        String normalizedAlias = this.messageNormalizer.normalize(alias);
        Stop stop = new Stop(
                alias,
                normalizedAlias,
                new Coordinates(entry.get("stop_lat"), entry.get("stop_lon")));
        this.stops.put(alias, stop);
    }

    private String removeVbbDetails(String originalStopName) {
        return originalStopName
                        .replaceAll("(S )|(S\\+U )|(U )", "")
                        .replaceAll("\\(.*\\).*", "")
                        .replaceAll("\\[.*\\].*", "")
                        .replace("Bhf", "");
    }

    private boolean isStation(String n) {
        return n.startsWith("S ") || n.startsWith("U ") || n.startsWith("S+U ");
    }

    @Override
    public Map<String, Stop> getStops() {
        return this.stops;
    }

    @Override
    public List<String> getStopNames() {
        return new ArrayList<>(this.stops.keySet());
    }
}
