package com.aljodomo.kontis.gtfs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Aljoscha Domonell
 */
@Data
@Configuration
@ConfigurationProperties("gtfs.filter")
public class GTFSFilterProps {
    private final List<String> agencyWhitelist = new ArrayList<>();

    private String routeShortNameRegEx;
}
