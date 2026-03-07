package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.in.BuildRaptorDataUseCase;
import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.domain.model.raptor.*;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RaptorDataBuilderService implements BuildRaptorDataUseCase {

    private final RaptorDataQueryPort raptorDataQueryPort;

    @Override
    public RaptorDataSet buildForCity(Short cityId) {
        List<StopEntity> stopEntities = raptorDataQueryPort.findStopsByCityId(cityId);
        List<TripEntity> tripEntities = raptorDataQueryPort.findTripsByCityId(cityId);
        List<StopTimeEntity> stopTimeEntities = raptorDataQueryPort.findStopTimesByCityId(cityId);

        Map<Long, List<StopTimeEntity>> stopTimesByTripId = groupStopTimesByTrip(stopTimeEntities);
        Map<PatternKey, List<TripEntity>> tripsByPattern = groupTripsByPattern(tripEntities, stopTimesByTripId);

        Map<Integer, RouteRaptor> routesById = new LinkedHashMap<>();
        Map<Integer, List<RouteAtStopRaptor>> routesAtStops = new HashMap<>();

        int nextRouteRaptorId = 0;

        for (Map.Entry<PatternKey, List<TripEntity>> entry : tripsByPattern.entrySet()) {
            PatternKey patternKey = entry.getKey();
            List<TripEntity> trips = entry.getValue();

            TripEntity representativeTrip = trips.getFirst();
            List<StopTimeEntity> representativeStopTimes = stopTimesByTripId.get(representativeTrip.getId());

            int[] stopIds = representativeStopTimes.stream()
                    .mapToInt(st -> Math.toIntExact(st.getStop().getId()))
                    .toArray();

            List<TripRaptor> tripRaptors = trips.stream()
                    .map(trip -> toTripRaptor(trip, stopTimesByTripId.get(trip.getId())))
                    .sorted(Comparator.comparingInt(t -> t.departureTimes()[0]))
                    .toList();

            RouteRaptor routeRaptor = new RouteRaptor(
                    nextRouteRaptorId,
                    representativeTrip.getRoute().getId(),
                    representativeTrip.getDirectionId(),
                    resolveHeadsign(representativeTrip, representativeStopTimes),
                    stopIds,
                    tripRaptors
            );

            routesById.put(nextRouteRaptorId, routeRaptor);

            for (int stopIndex = 0; stopIndex < stopIds.length; stopIndex++) {
                int stopId = stopIds[stopIndex];
                routesAtStops.computeIfAbsent(stopId, ignored -> new ArrayList<>())
                        .add(new RouteAtStopRaptor(nextRouteRaptorId, stopIndex));
            }

            nextRouteRaptorId++;
        }

        Map<Integer, StopRaptor> stopsById = new LinkedHashMap<>();
        for (StopEntity stopEntity : stopEntities) {
            int stopId = Math.toIntExact(stopEntity.getId());

            stopsById.put(
                    stopId,
                    new StopRaptor(
                            stopId,
                            stopEntity.getName(),
                            routesAtStops.getOrDefault(stopId, List.of()),
                            List.of()
                    )
            );
        }

        return new RaptorDataSet(
                stopsById,
                routesById,
                new ArrayList<>(routesById.keySet())
        );
    }

    private Map<Long, List<StopTimeEntity>> groupStopTimesByTrip(List<StopTimeEntity> stopTimeEntities) {
        Map<Long, List<StopTimeEntity>> result = new LinkedHashMap<>();

        for (StopTimeEntity stopTime : stopTimeEntities) {
            result.computeIfAbsent(stopTime.getTrip().getId(), ignored -> new ArrayList<>())
                    .add(stopTime);
        }

        result.values().forEach(list ->
                list.sort(Comparator.comparingInt(StopTimeEntity::getStopSequence)));

        return result;
    }

    private Map<PatternKey, List<TripEntity>> groupTripsByPattern(
            List<TripEntity> tripEntities,
            Map<Long, List<StopTimeEntity>> stopTimesByTripId
    ) {
        Map<PatternKey, List<TripEntity>> result = new LinkedHashMap<>();

        for (TripEntity trip : tripEntities) {
            List<StopTimeEntity> stopTimes = stopTimesByTripId.get(trip.getId());
            if (stopTimes == null || stopTimes.isEmpty()) {
                continue;
            }

            List<Long> stopSequencePattern = stopTimes.stream()
                    .map(st -> st.getStop().getId())
                    .toList();

            PatternKey key = new PatternKey(
                    trip.getRoute().getId(),
                    trip.getDirectionId(),
                    normalizeHeadsign(trip.getHeadsign()),
                    stopSequencePattern
            );

            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(trip);
        }

        return result;
    }

    private TripRaptor toTripRaptor(TripEntity trip, List<StopTimeEntity> stopTimes) {
        int[] arrivalTimes = stopTimes.stream()
                .mapToInt(st -> toSeconds(st.getArrivalTime()))
                .toArray();

        int[] departureTimes = stopTimes.stream()
                .mapToInt(st -> toSeconds(st.getDepartureTime()))
                .toArray();

        return new TripRaptor(
                Math.toIntExact(trip.getId()),
                arrivalTimes,
                departureTimes
        );
    }

    private int toSeconds(LocalTime time) {
        return time.toSecondOfDay();
    }

    private String resolveHeadsign(TripEntity trip, List<StopTimeEntity> stopTimes) {
        if (trip.getHeadsign() != null && !trip.getHeadsign().isBlank()) {
            return trip.getHeadsign();
        }

        StopTimeEntity last = stopTimes.get(stopTimes.size() - 1);
        return last.getStop().getName();
    }

    private String normalizeHeadsign(String headsign) {
        return headsign == null ? "" : headsign.trim();
    }

    private record PatternKey(
            Long routeId,
            Integer directionId,
            String headsign,
            List<Long> stopIds
    ) {}
}
