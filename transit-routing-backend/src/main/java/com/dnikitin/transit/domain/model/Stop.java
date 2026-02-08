package com.dnikitin.transit.domain.model;

public record Stop(
        Long id,
        String name,
        double lat,
        double lon
) {
}
