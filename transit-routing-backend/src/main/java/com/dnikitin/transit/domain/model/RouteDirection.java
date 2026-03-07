package com.dnikitin.transit.domain.model;

import java.util.List;

public record RouteDirection(
        Integer directionId,
        String headsign,
        List<Stop> stops
) {}
