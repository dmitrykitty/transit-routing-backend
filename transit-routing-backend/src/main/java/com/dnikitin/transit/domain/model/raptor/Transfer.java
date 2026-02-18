package com.dnikitin.transit.domain.model.raptor;

public record Transfer(
        int destinationStopId,
        int durationInSeconds
) {
}
