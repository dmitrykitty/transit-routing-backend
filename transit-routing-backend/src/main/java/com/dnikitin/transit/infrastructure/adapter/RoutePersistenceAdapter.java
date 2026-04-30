package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.RouteQueryPort;
import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.domain.model.RouteDetails;
import com.dnikitin.transit.domain.model.RouteDirection;
import com.dnikitin.transit.domain.model.Stop;
import com.dnikitin.transit.domain.model.VehicleType;
import com.dnikitin.transit.infrastructure.persistence.entity.*;
import com.dnikitin.transit.infrastructure.persistence.mapper.RouteEntityMapper;
import com.dnikitin.transit.infrastructure.persistence.mapper.StopEntityMapper;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RoutePersistenceAdapter implements RouteQueryPort {
    private final RouteJpaRepository routeRepository;
    private final CityJpaRepository cityRepository;
    private final TripJpaRepository tripRepository;
    private final StopTimeJpaRepository stopTimeRepository;
    private final RouteEntityMapper routeEntityMapper;
    private final StopEntityMapper stopEntityMapper;

    @Override
    public List<Route> findRoutesByCityId(Short cityId) {
        CityEntity city = getCityOrThrow(cityId);

        return routeRepository.findAllByCity(city).stream()
                .map(routeEntityMapper::toRouteSummary)
                .toList();
    }

    @Override
    public List<Route> findRoutesByCityIdAndVehicleType(Short cityId, VehicleType vehicleType) {
        CityEntity city = getCityOrThrow(cityId);

        return routeRepository.findAllByCityAndVehicleType(city, toEntityVehicleType(vehicleType)).stream()
                .map(routeEntityMapper::toRouteSummary)
                .toList();
    }

    @Override
    public Optional<RouteDetails> findRouteByCityIdTypeAndRouteNumber(
            Short cityId,
            VehicleType type,
            String routeNumber
    ) {
        CityEntity city = getCityOrThrow(cityId);

        return routeRepository.findByCityAndVehicleTypeAndRouteNumber(city, toEntityVehicleType(type), routeNumber)
                .map(this::toRouteDetails);
    }

    private CityEntity getCityOrThrow(Short cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
    }

    private RouteDetails toRouteDetails(RouteEntity routeEntity) {
        List<TripEntity> trips = tripRepository.findAllByRoute(routeEntity);

        if (trips.isEmpty()) {
            return new RouteDetails(
                    routeEntity.getId(),
                    routeEntity.getRouteNumber(),
                    routeEntity.getName(),
                    List.of()
            );
        }

        List<StopTimeEntity> stopTimes = stopTimeRepository.findAllByTripsOrdered(trips);

        Map<Long, List<StopTimeEntity>> stopTimesByTripId = new LinkedHashMap<>();
        for (StopTimeEntity stopTime : stopTimes) {
            stopTimesByTripId
                    .computeIfAbsent(stopTime.getTrip().getId(), ignored -> new ArrayList<>())
                    .add(stopTime);
        }

        List<RouteDirection> directions = chooseRepresentativeDirections(trips, stopTimesByTripId);

        return new RouteDetails(
                routeEntity.getId(),
                routeEntity.getRouteNumber(),
                routeEntity.getName(),
                directions
        );
    }

    private List<RouteDirection> chooseRepresentativeDirections(
            List<TripEntity> trips,
            Map<Long, List<StopTimeEntity>> stopTimesByTripId
    ) {
        Map<Integer, List<TripEntity>> tripsByDirection = trips.stream()
                .collect(LinkedHashMap::new,
                        (map, trip) -> map.computeIfAbsent(normalizeDirectionId(trip.getDirectionId()), k -> new ArrayList<>()).add(trip),
                        Map::putAll);

        return tripsByDirection.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> buildDirection(entry.getKey(), entry.getValue(), stopTimesByTripId))
                .toList();
    }

    private RouteDirection buildDirection(
            Integer directionId,
            List<TripEntity> tripsForDirection,
            Map<Long, List<StopTimeEntity>> stopTimesByTripId
    ) {
        TripEntity representativeTrip = tripsForDirection.stream()
                .max(Comparator.comparingInt(trip ->
                        stopTimesByTripId.getOrDefault(trip.getId(), List.of()).size()))
                .orElseThrow();

        List<Stop> stops = stopTimesByTripId.getOrDefault(representativeTrip.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(StopTimeEntity::getStopSequence))
                .map(stopTime -> stopEntityMapper.toStop(stopTime.getStop()))
                .toList();

        String headsign = representativeTrip.getHeadsign();
        if (headsign == null || headsign.isBlank()) {
            headsign = stops.isEmpty() ? null : stops.get(stops.size() - 1).name();
        }

        return new RouteDirection(
                directionId,
                headsign,
                stops
        );
    }

    private Integer normalizeDirectionId(Integer directionId) {
        return directionId == null ? -1 : directionId;
    }

    private com.dnikitin.transit.infrastructure.persistence.entity.VehicleType toEntityVehicleType(VehicleType vehicleType) {
        return com.dnikitin.transit.infrastructure.persistence.entity.VehicleType.valueOf(vehicleType.name());
    }
}
