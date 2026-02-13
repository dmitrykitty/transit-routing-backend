package com.dnikitin.transit.domain.model;


import lombok.Builder;

@Builder
public record Connection(
        String departureStopId,
        String arrivalStopId,
        Integer departureTime,
        Integer arrivalTime,
        String tripId) {
}
