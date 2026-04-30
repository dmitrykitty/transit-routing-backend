package com.dnikitin.transit.api.dto.response;

import java.time.LocalTime;

public record RaptorJourneyLegResponse(
        String type,
        int fromStopId,
        String fromStopName,
        int toStopId,
        String toStopName,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Long sourceRouteId,
        Integer tripId,
        String headsign
) {
}
