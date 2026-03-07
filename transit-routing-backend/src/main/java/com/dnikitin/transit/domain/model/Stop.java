package com.dnikitin.transit.domain.model;

import lombok.Builder;

@Builder
public record Stop(
        Long id,
        String idExternal,
        String name,
        Double lat,
        Double lon,
        String stopCode
) {
}
