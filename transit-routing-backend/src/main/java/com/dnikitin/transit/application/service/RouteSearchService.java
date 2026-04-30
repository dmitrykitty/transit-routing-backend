package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.in.GetRoutesUseCase;
import com.dnikitin.transit.application.port.out.RouteQueryPort;
import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.domain.model.RouteDetails;
import com.dnikitin.transit.domain.model.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteSearchService implements GetRoutesUseCase {
    private final RouteQueryPort routeQueryPort;

    @Override
    public List<Route> getRoutesForCity(Short cityId) {
        return routeQueryPort.findRoutesByCityId(cityId);
    }

    @Override
    public List<Route> getRoutesByCityAndVehicleType(Short cityId, VehicleType vehicleType) {
        return routeQueryPort.findRoutesByCityIdAndVehicleType(cityId, vehicleType);
    }

    @Override
    public Optional<RouteDetails> getRouteByCityVehicleTypeAndRouteNumber(
            Short cityId,
            VehicleType type,
            String routeNumber
    ) {
        return routeQueryPort.findRouteByCityIdTypeAndRouteNumber(cityId, type, routeNumber);
    }
}
