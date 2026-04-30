package com.dnikitin.transit.domain.model;

public record RouteSummary(
        Long id,
        String routeNumber,
        String name,
        VehicleType vehicleType
) {}
