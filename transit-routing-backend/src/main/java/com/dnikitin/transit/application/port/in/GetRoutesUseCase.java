package com.dnikitin.transit.application.port.in;

import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.domain.model.RouteDetails;
import com.dnikitin.transit.infrastructure.persistence.entity.VehicleType;

import java.util.List;
import java.util.Optional;

public interface GetRoutesUseCase {
    List<Route> getRoutesForCity(Short cityId);

    List<Route> getRoutesByCityAndVehicleType(Short cityId, VehicleType vehicleType);

    Optional<RouteDetails> getRouteByCityVehicleTypeAndRouteNumber(
            Short cityId,
            VehicleType vehicleType,
            String routeNumber
    );
}
