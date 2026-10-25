package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.in.BuildRaptorDataUseCase;
import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopData;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopTimeData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTransferData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import com.dnikitin.transit.domain.model.raptor.*;
import com.dnikitin.transit.domain.service.FifoTripPartitioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RaptorDataBuilderService implements BuildRaptorDataUseCase {

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private final RaptorDataQueryPort raptorDataQueryPort;
    private final RaptorRoutingProperties raptorRoutingProperties;
    private final Map<CacheKey, RaptorDataSet> cache = new ConcurrentHashMap<>();

    @Override
    public RaptorDataSet buildForCity(Short cityId) {
        return buildForCity(cityId, null);
    }

    @Override
    public RaptorDataSet buildForCity(Short cityId, LocalDate serviceDate) {
        return cache.computeIfAbsent(new CacheKey(cityId, serviceDate), ignored -> buildUncached(cityId, serviceDate));
    }

    @Override
    public void invalidateCity(Short cityId) {
        cache.keySet().removeIf(key -> key.cityId().equals(cityId));
    }

    private RaptorDataSet buildUncached(Short cityId, LocalDate serviceDate) {
        List<RaptorStopData> stops = raptorDataQueryPort.findStopsByCityId(cityId);
        List<RaptorTripData> trips = loadTrips(cityId, serviceDate);
        List<RaptorTransferData> transfers = loadTransfers(cityId);

        Map<PatternKey, List<RaptorTripData>> tripsByPattern = groupTripsByPattern(trips);

        Map<Integer, RouteRaptor> routesById = new LinkedHashMap<>();
        Map<Integer, List<RouteAtStopRaptor>> routesAtStops = new HashMap<>();

        int nextRouteRaptorId = 0;

        for (Map.Entry<PatternKey, List<RaptorTripData>> entry : tripsByPattern.entrySet()) {
            List<RaptorTripData> routeTrips = entry.getValue();

            RaptorTripData representativeTrip = routeTrips.getFirst();
            List<RaptorStopTimeData> representativeStopTimes = representativeTrip.stopTimes();

            int[] stopIds = representativeStopTimes.stream()
                    .mapToInt(st -> Math.toIntExact(st.stopId()))
                    .toArray();

            List<TripRaptor> tripRaptors = routeTrips.stream()
                    .map(this::toTripRaptor)
                    .toList();

            // The router binary-searches trips, so a pattern whose trips overtake each other becomes
            // several RAPTOR routes with the same stops.
            for (List<TripRaptor> fifoTrips : FifoTripPartitioner.partition(tripRaptors)) {
                RouteRaptor routeRaptor = new RouteRaptor(
                        nextRouteRaptorId,
                        representativeTrip.sourceRouteId(),
                        representativeTrip.directionId(),
                        resolveHeadsign(representativeTrip, representativeStopTimes),
                        stopIds,
                        fifoTrips
                );

                routesById.put(nextRouteRaptorId, routeRaptor);

                for (int stopIndex = 0; stopIndex < stopIds.length; stopIndex++) {
                    int stopId = stopIds[stopIndex];
                    routesAtStops.computeIfAbsent(stopId, ignored -> new ArrayList<>())
                            .add(new RouteAtStopRaptor(nextRouteRaptorId, stopIndex));
                }

                nextRouteRaptorId++;
            }
        }

        Map<Integer, StopRaptor> stopsById = new LinkedHashMap<>();
        Map<Long, List<TransferRaptor>> transfersByStopId = groupTransfersByStop(transfers);
        for (RaptorStopData stop : stops) {
            int stopId = Math.toIntExact(stop.id());

            stopsById.put(
                    stopId,
                    new StopRaptor(
                            stopId,
                            stop.name(),
                            routesAtStops.getOrDefault(stopId, List.of()),
                            transfersByStopId.getOrDefault(stop.id(), List.of())
                    )
            );
        }

        return new RaptorDataSet(
                stopsById,
                routesById,
                new ArrayList<>(routesById.keySet())
        );
    }

    /**
     * Trips of the service date plus the previous service day's trips that are still running after
     * midnight, shifted by -24h onto this date's time axis (GTFS times past 24:00 belong to the day
     * the trip started). A shifted trip keeps only the stops from its first departure at or after
     * 00:00: a query on this date cannot board earlier, so the stops before midnight are dropped.
     */
    private List<RaptorTripData> loadTrips(Short cityId, LocalDate serviceDate) {
        List<RaptorTripData> trips = raptorDataQueryPort.findTripsByCityId(cityId, serviceDate);
        if (serviceDate == null) {
            return trips;
        }

        List<RaptorTripData> result = new ArrayList<>(trips);
        for (RaptorTripData previousDayTrip : raptorDataQueryPort.findTripsByCityId(cityId, serviceDate.minusDays(1))) {
            RaptorTripData shifted = shiftToNextServiceDay(previousDayTrip);
            if (shifted != null) {
                result.add(shifted);
            }
        }

        return result;
    }

    private RaptorTripData shiftToNextServiceDay(RaptorTripData trip) {
        List<RaptorStopTimeData> stopTimes = trip.stopTimes();
        if (stopTimes == null) {
            return null;
        }

        List<RaptorStopTimeData> shiftedStopTimes = stopTimes.stream()
                .dropWhile(st -> st.departureSeconds() < SECONDS_PER_DAY)
                .map(st -> new RaptorStopTimeData(
                        st.stopId(),
                        st.stopName(),
                        Math.max(0, st.arrivalSeconds() - SECONDS_PER_DAY),
                        st.departureSeconds() - SECONDS_PER_DAY,
                        st.stopSequence()
                ))
                .toList();

        // A single remaining stop cannot be ridden to anywhere.
        if (shiftedStopTimes.size() < 2) {
            return null;
        }

        return new RaptorTripData(
                trip.id(),
                trip.sourceRouteId(),
                trip.directionId(),
                trip.headsign(),
                shiftedStopTimes
        );
    }

    private Map<PatternKey, List<RaptorTripData>> groupTripsByPattern(List<RaptorTripData> trips) {
        Map<PatternKey, List<RaptorTripData>> result = new LinkedHashMap<>();

        for (RaptorTripData trip : trips) {
            List<RaptorStopTimeData> stopTimes = trip.stopTimes();
            if (stopTimes == null || stopTimes.isEmpty()) {
                continue;
            }

            List<Long> stopSequencePattern = stopTimes.stream()
                    .map(RaptorStopTimeData::stopId)
                    .toList();

            PatternKey key = new PatternKey(
                    trip.sourceRouteId(),
                    trip.directionId(),
                    normalizeHeadsign(trip.headsign()),
                    stopSequencePattern
            );

            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(trip);
        }

        return result;
    }

    private TripRaptor toTripRaptor(RaptorTripData trip) {
        int[] arrivalTimes = trip.stopTimes().stream()
                .mapToInt(RaptorStopTimeData::arrivalSeconds)
                .toArray();

        int[] departureTimes = trip.stopTimes().stream()
                .mapToInt(RaptorStopTimeData::departureSeconds)
                .toArray();

        return new TripRaptor(
                Math.toIntExact(trip.id()),
                arrivalTimes,
                departureTimes
        );
    }

    private String resolveHeadsign(RaptorTripData trip, List<RaptorStopTimeData> stopTimes) {
        if (trip.headsign() != null && !trip.headsign().isBlank()) {
            return trip.headsign();
        }

        RaptorStopTimeData last = stopTimes.get(stopTimes.size() - 1);
        return last.stopName();
    }

    private String normalizeHeadsign(String headsign) {
        return headsign == null ? "" : headsign.trim();
    }

    private List<RaptorTransferData> loadTransfers(Short cityId) {
        RaptorRoutingProperties.Transfer transfer = raptorRoutingProperties.getTransfer();
        if (!transfer.isEnabled()) {
            return List.of();
        }

        return raptorDataQueryPort.findTransfersByCityId(
                cityId,
                transfer.getRadiusMeters(),
                transfer.getWalkingSpeedMetersPerSecond(),
                transfer.getMaxDurationSeconds()
        );
    }

    private Map<Long, List<TransferRaptor>> groupTransfersByStop(List<RaptorTransferData> transfers) {
        Map<Long, List<TransferRaptor>> result = new HashMap<>();

        for (RaptorTransferData transfer : transfers) {
            result.computeIfAbsent(transfer.fromStopId(), ignored -> new ArrayList<>())
                    .add(new TransferRaptor(
                            Math.toIntExact(transfer.toStopId()),
                            transfer.durationInSeconds()
                    ));
        }

        return result;
    }

    private record PatternKey(
            Long routeId,
            Integer directionId,
            String headsign,
            List<Long> stopIds
    ) {}

    private record CacheKey(
            Short cityId,
            LocalDate serviceDate
    ) {}
}
