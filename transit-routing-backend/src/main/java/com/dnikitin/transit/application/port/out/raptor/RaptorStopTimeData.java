package com.dnikitin.transit.application.port.out.raptor;

import java.time.LocalTime;

public record RaptorStopTimeData(
        long stopId,
        String stopName,
        LocalTime arrivalTime,
        LocalTime departureTime,
        int stopSequence
) {
}
