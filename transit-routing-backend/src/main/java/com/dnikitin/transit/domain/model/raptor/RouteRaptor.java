package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;
import java.util.List;

public record RouteRaptor(
        int id,
        int[] stopIds,
        List<TripRaptor> tripRaptors


) implements Serializable {
}
