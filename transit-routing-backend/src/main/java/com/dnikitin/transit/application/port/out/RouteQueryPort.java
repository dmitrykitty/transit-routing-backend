package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.domain.model.RouteDetails;
import com.dnikitin.transit.domain.model.VehicleType;

import java.util.List;
import java.util.Optional;

public interface RouteQueryPort {
    List<Route> findRoutesByCityId(Short cityId);

    List<Route> findRoutesByCityIdAndVehicleType(Short cityId, VehicleType vehicleType);

    Optional<RouteDetails> findRouteByCityIdTypeAndRouteNumber(
            Short cityId,
            VehicleType type,
            String routeNumber
    );
}
