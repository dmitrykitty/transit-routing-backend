package com.dnikitin.transit.application.port.out.raptor;

public record RaptorStopData(
        long id,
        String name,
        double latitude,
        double longitude
) {
}
