package com.dnikitin.transit.domain.model.raptor;

import java.time.LocalTime;
import java.util.List;

public record RaptorJourney(
        int sourceStopId,
        int targetStopId,
        LocalTime requestedDepartureTime,
        LocalTime arrivalTime,
        int tripCount,
        int transferCount,
        List<RaptorJourneyLeg> legs
) {
}
