package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;

public record RouteAtStopRaptor(
        int routeId,
        int stopIndexInRoute
) implements Serializable {
}