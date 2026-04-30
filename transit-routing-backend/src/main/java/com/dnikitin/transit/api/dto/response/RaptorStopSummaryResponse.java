package com.dnikitin.transit.api.dto.response;

public record RaptorStopSummaryResponse(
        int stopId,
        String stopName,
        int routeCount,
        int transferCount
) {
}
