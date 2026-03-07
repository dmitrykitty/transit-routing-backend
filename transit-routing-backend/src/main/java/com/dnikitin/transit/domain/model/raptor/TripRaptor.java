package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;

public record TripRaptor(
        int[] arrivalTimes,
        int[] departureTimes
) implements Serializable {
}
