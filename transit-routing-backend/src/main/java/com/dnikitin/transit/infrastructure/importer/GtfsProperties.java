package com.dnikitin.transit.infrastructure.importer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "gtfs")
@Data
public class GtfsProperties {
    // Miasto -> (SourceId -> URL)
    private Map<String, Map<String, String>> cities;
}