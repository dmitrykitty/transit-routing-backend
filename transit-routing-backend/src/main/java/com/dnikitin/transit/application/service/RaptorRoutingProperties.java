package com.dnikitin.transit.application.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "raptor")
@Data
public class RaptorRoutingProperties {

    private Transfer transfer = new Transfer();

    @Data
    public static class Transfer {
        private boolean enabled = true;
        private double radiusMeters = 150.0;
        private double walkingSpeedMetersPerSecond = 1.4;
        private int maxDurationSeconds = 300;
    }
}
