package com.dnikitin.transit.application.service;

import com.dnikitin.transit.infrastructure.persistence.entity.RouteStopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.RouteStopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TripSearchService {
    private final RouteStopJpaRepository routeStopRepository;
    private final TripJpaRepository tripRepository;


    public TripSearchService(RouteStopJpaRepository routeStopRepository, TripJpaRepository tripRepository) {
        this.routeStopRepository = routeStopRepository;
        this.tripRepository = tripRepository;
    }

    public List<TripEntity> findDirectTrips(
            long fromStopId,
            long toStopId,
            OffsetDateTime fromTime,
            OffsetDateTime toTime) {

        List<TripEntity> tripsByTime = tripRepository.findTripsByTime(fromTime, toTime);

        List<Long> listOfRouteIds = tripsByTime.stream()
                .map(tripEntity -> tripEntity.getRoute().getId())
                .distinct()
                .toList();

        List<RouteStopEntity> byRouteIdsWithStops = routeStopRepository.findByRouteIdsWithStops(listOfRouteIds);

        return tripsByTime;
    }

}
