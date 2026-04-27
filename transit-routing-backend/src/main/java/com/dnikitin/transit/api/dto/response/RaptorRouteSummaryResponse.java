package com.dnikitin.transit.api.dto.response;

public record RaptorRouteSummaryResponse(
        int routeId,
        Long sourceRouteId,
        Integer directionId,
        String headsign,
        int stopCount,
        int tripCount,
        Integer firstStopId,
        Integer lastStopId
) {
}
