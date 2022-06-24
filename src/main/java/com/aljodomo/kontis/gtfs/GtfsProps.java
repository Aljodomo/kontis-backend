package com.aljodomo.kontis.gtfs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Aljoscha Domonell
 */
@Data
@Configuration
@ConfigurationProperties("gtfs")
public class GtfsProps {
    private String location;
}
