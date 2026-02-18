package com.dnikitin.transit.domain.model.raptor;

public record Trip(
        int[] arrivalTimes,
        int[] departureTimes
) {
}
