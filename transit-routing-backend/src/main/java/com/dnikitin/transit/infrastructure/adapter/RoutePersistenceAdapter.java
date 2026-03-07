package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.RouteQueryPort;
import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.VehicleType;
import com.dnikitin.transit.infrastructure.persistence.mapper.RouteEntityMapper;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoutePersistenceAdapter implements RouteQueryPort {

    private final RouteJpaRepository routeRepository;
    private final CityJpaRepository cityRepository;
    private final RouteEntityMapper routeEntityMapper;

    @Override
    public List<Route> findRoutesByCityId(Short cityId) {
        CityEntity city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
        return routeRepository.findAllByCity(city).stream()
                .map(routeEntityMapper::toRoute)
                .toList();
    }

    @Override
    public List<Route> findRoutesByCityIdAndVehicleType(Short cityId, VehicleType vehicleType) {
        CityEntity city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
        return routeRepository.findAllByCityAndVehicleType(city, vehicleType).stream()
                .map(routeEntityMapper::toRoute)
                .toList();
    }

    @Override
    public Optional<Route> findRouteByCityIdTypeAndRouteNumber(Short cityId, VehicleType type, String routeNumber) {
        CityEntity city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
        return routeRepository.findByCityAndVehicleTypeAndRouteNumber(city, type, routeNumber)
                .map(routeEntityMapper::toRoute);
    }
}
