package com.dnikitin.transit.api.dto.response;

import com.dnikitin.transit.domain.model.VehicleType;

public record RouteSummaryResponse(
        Long id,
        String routeNumber
) {
}
