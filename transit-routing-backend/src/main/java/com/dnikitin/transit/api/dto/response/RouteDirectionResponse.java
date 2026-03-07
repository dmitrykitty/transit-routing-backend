package com.dnikitin.transit.api.dto.response;

import java.util.List;

public record RouteDirectionResponse(
        Integer directionId,
        String headsign,
        List<StopResponse> stops
) {
}
