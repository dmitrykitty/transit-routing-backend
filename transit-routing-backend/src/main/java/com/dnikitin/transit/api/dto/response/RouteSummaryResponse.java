package com.dnikitin.transit.api.dto.response;

public record RouteSummaryResponse(
        Long id,
        String routeNumber,
        String name
) {
}
