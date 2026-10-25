package com.dnikitin.transit.application.port.out.raptor;

public record RaptorStopTimeData(
        long stopId,
        String stopName,
        int arrivalSeconds,
        int departureSeconds,
        int stopSequence
) {
}
