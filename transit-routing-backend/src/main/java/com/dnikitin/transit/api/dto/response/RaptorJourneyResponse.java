package com.dnikitin.transit.api.dto.response;

import java.time.LocalTime;
import java.util.List;

public record RaptorJourneyResponse(
        int sourceStopId,
        int targetStopId,
        LocalTime requestedDepartureTime,
        LocalTime arrivalTime,
        int tripCount,
        int transferCount,
        List<RaptorJourneyLegResponse> legs
) {
}
