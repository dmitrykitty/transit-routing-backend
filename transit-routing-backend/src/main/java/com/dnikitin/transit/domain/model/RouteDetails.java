package com.dnikitin.transit.domain.model;

import java.util.List;

public record RouteDetails(
        Long id,
        String routeNumber,
        String name,
        List<RouteDirection> directions
) {
}
