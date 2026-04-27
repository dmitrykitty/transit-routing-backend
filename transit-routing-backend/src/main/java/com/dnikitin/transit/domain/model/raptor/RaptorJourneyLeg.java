package com.dnikitin.transit.domain.model.raptor;

import java.time.LocalTime;

public record RaptorJourneyLeg(
        JourneyLegType type,
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
