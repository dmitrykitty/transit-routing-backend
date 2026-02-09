package com.dnikitin.transit.api.dto.response;

public record StopResponse(
        Long id,
        String name,
        double lat,
        double lon
) {
}
