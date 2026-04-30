package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;
import java.util.List;

public record RouteRaptor(
        int id,
        Long sourceRouteId,
        Integer directionId,
        String headsign,
        int[] stopIds,
        List<TripRaptor> trips
) implements Serializable {
}
