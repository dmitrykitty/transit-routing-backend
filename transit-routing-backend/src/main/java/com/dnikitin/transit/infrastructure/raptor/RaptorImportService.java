package com.dnikitin.transit.infrastructure.raptor;

import com.dnikitin.transit.application.port.out.RaptorRepositoryPort;
import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.StopRaptor;
import com.dnikitin.transit.domain.model.raptor.TransferRaptor;
import com.dnikitin.transit.domain.model.raptor.TripRaptor;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaptorImportService {

    private final TripJpaRepository tripRepository;
    private final StopJpaRepository stopRepository;
    private final StopTimeJpaRepository stopTimeRepository;

    private final RaptorRepositoryPort raptorRepositoryPort;

    @Transactional(readOnly = true)
    public void importDataToRedis(CityEntity city) {
        log.info("Starting RAPTOR data import for city '{}' (ID: {})...", city.getName(), city.getId());

        raptorRepositoryPort.clearCityData(city.getId());

        List<StopEntity> allStops = stopRepository.findAllByCity(city);
        List<TripEntity> allTrips = tripRepository.findAllByCity(city);
        List<StopTimeEntity> allStopTimes = stopTimeRepository.findAllByCity(city);

        Map<Long, List<StopTimeEntity>> stopTimesByTripId = allStopTimes.stream()
                .collect(Collectors.groupingBy(st -> st.getTrip().getId()));

        Map<Long, Integer> dbToRaptorId = new HashMap<>();
        Map<Integer, Long> raptorToDbId = new HashMap<>();

        int stopIndex = 0;
        for (StopEntity stop : allStops) {
            dbToRaptorId.put(stop.getId(), stopIndex);
            raptorToDbId.put(stopIndex, stop.getId());
            stopIndex++;
        }
        raptorRepositoryPort.saveStopMapping(city.getId(), raptorToDbId);

        Map<List<Integer>, List<TripEntity>> routeGroups = new HashMap<>();

        for (TripEntity trip : allTrips) {
            List<StopTimeEntity> tripStopTimes = stopTimesByTripId.getOrDefault(trip.getId(), List.of());
            if (tripStopTimes.isEmpty()) {
                continue;
            }

            List<Integer> raptorStopPattern = tripStopTimes.stream()
                    .sorted(Comparator.comparingInt(StopTimeEntity::getStopSequence))
                    .map(st -> dbToRaptorId.get(st.getStop().getId()))
                    .collect(Collectors.toList());

            if (raptorStopPattern.contains(null)) {
                log.warn("Trip {} contains unknown stops. Skipping.", trip.getId());
                continue;
            }

            routeGroups.computeIfAbsent(raptorStopPattern, k -> new ArrayList<>()).add(trip);
        }

        int routeIdCounter = 0;
        Map<Integer, List<Integer>> stopToRouteMap = new HashMap<>();

        for (Map.Entry<List<Integer>, List<TripEntity>> entry : routeGroups.entrySet()) {
            List<Integer> stopIds = entry.getKey();
            List<TripEntity> trips = entry.getValue();

            int raptorRouteId = routeIdCounter++;
            RouteRaptor route = createRaptorRoute(raptorRouteId, stopIds, trips, stopTimesByTripId);
            raptorRepositoryPort.saveRoute(city.getId(), route);

            for (Integer raptorStopId : stopIds) {
                stopToRouteMap.computeIfAbsent(raptorStopId, k -> new ArrayList<>()).add(raptorRouteId);
            }
        }

        for (StopEntity stopEntity : allStops) {
            Integer raptorId = dbToRaptorId.get(stopEntity.getId());
            StopRaptor raptorStop = new StopRaptor(
                    raptorId,
                    stopToRouteMap.getOrDefault(raptorId, List.of()),
                    List.<TransferRaptor>of()
            );
            raptorRepositoryPort.saveStop(city.getId(), raptorStop);
        }

        raptorRepositoryPort.markDataAvailable(city.getId());
        log.info("Imported {} stops and {} routes for city '{}'.", allStops.size(), routeIdCounter, city.getName());
    }

    private RouteRaptor createRaptorRoute(
            int id,
            List<Integer> stopIds,
            List<TripEntity> trips,
            Map<Long, List<StopTimeEntity>> stopTimesByTripId
    ) {
        int[] routeStopIds = stopIds.stream().mapToInt(i -> i).toArray();

        List<TripRaptor> raptorTrips = new ArrayList<>();

        for (TripEntity entity : trips) {
            List<StopTimeEntity> sortedTimes = stopTimesByTripId.getOrDefault(entity.getId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(StopTimeEntity::getStopSequence))
                    .toList();

            if (sortedTimes.isEmpty()) {
                continue;
            }

            int[] arrivalTimes = sortedTimes.stream()
                    .mapToInt(st -> st.getArrivalTime().toSecondOfDay())
                    .toArray();
            int[] departureTimes = sortedTimes.stream()
                    .mapToInt(this::convertToSeconds)
                    .toArray();

            raptorTrips.add(new TripRaptor(arrivalTimes, departureTimes));
        }

        raptorTrips.sort(Comparator.comparingInt(t -> t.departureTimes()[0]));
        return new RouteRaptor(id, routeStopIds, raptorTrips);
    }

    private int convertToSeconds(StopTimeEntity st) {
        return st.getDepartureTime().toSecondOfDay();
    }


}
