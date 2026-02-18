package com.dnikitin.transit.domain.model.raptor;

import java.util.List;


public record Route(
        int id,
        int[] stopIds,
        List<Trip> trips
) {
}
