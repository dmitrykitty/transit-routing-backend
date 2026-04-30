package com.dnikitin.transit.application.port.out.raptor;

import java.util.List;

public record RaptorTripData(
        long id,
        long sourceRouteId,
        Integer directionId,
        String headsign,
        List<RaptorStopTimeData> stopTimes
) {
}
