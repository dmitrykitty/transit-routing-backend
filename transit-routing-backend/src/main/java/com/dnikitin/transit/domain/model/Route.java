package com.dnikitin.transit.domain.model;

import java.util.List;

public record Route(
        Long id,
        String idExternal,
        String routeNumber,
        String name,
        List<Stop>stops
) {
}
