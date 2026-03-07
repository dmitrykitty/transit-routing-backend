package com.dnikitin.transit.api.dto.response;

import java.util.List;

public record RouteDetailsResponse(
        Long id,
        String routeNumber,
        List<RouteDirectionResponse> directions
) {
}
