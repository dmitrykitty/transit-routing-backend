package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.*;

import java.util.*;

/**
 * Small transit network for router tests, kept independent of the RAPTOR data layout so the
 * brute-force reference does not share code with the router. {@link #toDataSet()} converts it the
 * same way RaptorDataBuilderService does (overtaking trips split by {@link FifoTripPartitioner},
 * stop index per route).
 */
final class TestNetwork {

    record Trip(int id, int[] arrivals, int[] departures) {
    }

    record Route(int id, int[] stopIds, List<Trip> trips) {
    }

    record Footpath(int fromStopId, int toStopId, int durationSeconds) {
    }

    final List<Integer> stopIds = new ArrayList<>();
    final List<Route> routes = new ArrayList<>();
    final List<Footpath> footpaths = new ArrayList<>();

    TestNetwork stops(int... ids) {
        for (int id : ids) {
            stopIds.add(id);
        }
        return this;
    }

    TestNetwork route(int id, int[] stops, Trip... trips) {
        routes.add(new Route(id, stops, List.of(trips)));
        return this;
    }

    TestNetwork footpath(int from, int to, int durationSeconds) {
        footpaths.add(new Footpath(from, to, durationSeconds));
        return this;
    }

    static Trip trip(int id, int... times) {
        return new Trip(id, times.clone(), times.clone());
    }

    RaptorDataSet toDataSet() {
        Map<Integer, List<RouteAtStopRaptor>> routesAtStop = new HashMap<>();
        Map<Integer, RouteRaptor> routesById = new LinkedHashMap<>();
        int nextRaptorRouteId = 0;
        for (Route route : routes) {
            List<TripRaptor> trips = route.trips().stream()
                    .map(trip -> new TripRaptor(trip.id(), trip.arrivals(), trip.departures()))
                    .toList();
            for (List<TripRaptor> fifoTrips : FifoTripPartitioner.partition(trips)) {
                int raptorRouteId = nextRaptorRouteId++;
                routesById.put(raptorRouteId, new RouteRaptor(raptorRouteId, (long) route.id(), 0, "R" + route.id(), route.stopIds(), fifoTrips));
                for (int i = 0; i < route.stopIds().length; i++) {
                    routesAtStop.computeIfAbsent(route.stopIds()[i], ignored -> new ArrayList<>())
                            .add(new RouteAtStopRaptor(raptorRouteId, i));
                }
            }
        }

        Map<Integer, List<TransferRaptor>> transfersFrom = new HashMap<>();
        for (Footpath footpath : footpaths) {
            transfersFrom.computeIfAbsent(footpath.fromStopId(), ignored -> new ArrayList<>())
                    .add(new TransferRaptor(footpath.toStopId(), footpath.durationSeconds()));
        }

        Map<Integer, StopRaptor> stopsById = new LinkedHashMap<>();
        for (int stopId : stopIds) {
            stopsById.put(stopId, new StopRaptor(stopId, "S" + stopId,
                    routesAtStop.getOrDefault(stopId, List.of()), transfersFrom.getOrDefault(stopId, List.of())));
        }
        return new RaptorDataSet(stopsById, routesById, new ArrayList<>(routesById.keySet()));
    }

    /**
     * Random network. Most routes have one travel-time profile shared by all trips (no overtaking, as in
     * the real feed for a single service date); about a third give each trip its own speed, so trips
     * can overtake each other. All times stay before 24:00.
     */
    static TestNetwork random(Random random) {
        TestNetwork network = new TestNetwork();
        int stopCount = 4 + random.nextInt(7);
        for (int i = 1; i <= stopCount; i++) {
            network.stops(i);
        }

        int routeCount = 1 + random.nextInt(5);
        int tripId = 1;
        for (int routeId = 1; routeId <= routeCount; routeId++) {
            List<Integer> shuffled = new ArrayList<>(network.stopIds);
            Collections.shuffle(shuffled, random);
            int length = 2 + random.nextInt(Math.min(4, stopCount - 1));
            int[] stops = shuffled.subList(0, length).stream().mapToInt(Integer::intValue).toArray();

            int[] travel = new int[length];
            int[] dwell = new int[length];
            for (int i = 1; i < length; i++) {
                travel[i] = 60 + random.nextInt(841);
                dwell[i] = random.nextInt(3) == 0 ? random.nextInt(61) : 0;
            }

            boolean mixedSpeeds = random.nextInt(3) == 0;
            int tripCount = 1 + random.nextInt(4);
            int firstDeparture = 6 * 3600 + random.nextInt(4 * 3600);
            List<Trip> trips = new ArrayList<>();
            for (int t = 0; t < tripCount; t++) {
                double speedFactor = mixedSpeeds ? 0.5 + random.nextDouble() : 1.0;
                int[] arrivals = new int[length];
                int[] departures = new int[length];
                arrivals[0] = firstDeparture;
                departures[0] = firstDeparture;
                for (int i = 1; i < length; i++) {
                    arrivals[i] = departures[i - 1] + (int) Math.round(travel[i] * speedFactor);
                    departures[i] = arrivals[i] + dwell[i];
                }
                trips.add(new Trip(tripId++, arrivals, departures));
                firstDeparture += 300 + random.nextInt(1501);
            }
            network.routes.add(new Route(routeId, stops, trips));
        }

        int footpathCount = random.nextInt(stopCount + 1);
        for (int i = 0; i < footpathCount; i++) {
            int from = 1 + random.nextInt(stopCount);
            int to = 1 + random.nextInt(stopCount);
            if (from == to) {
                continue;
            }
            int duration = 60 + random.nextInt(541);
            network.footpath(from, to, duration);
            if (random.nextBoolean()) {
                network.footpath(to, from, duration);
            }
        }
        return network;
    }

    int tripCount() {
        return routes.stream().mapToInt(route -> route.trips().size()).sum();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("stops=").append(stopIds).append('\n');
        for (Route route : routes) {
            text.append("route ").append(route.id()).append(' ').append(Arrays.toString(route.stopIds())).append('\n');
            for (Trip trip : route.trips()) {
                text.append("  trip ").append(trip.id())
                        .append(" arr=").append(Arrays.toString(trip.arrivals()))
                        .append(" dep=").append(Arrays.toString(trip.departures())).append('\n');
            }
        }
        footpaths.forEach(footpath -> text.append("footpath ").append(footpath).append('\n'));
        return text.toString();
    }
}
