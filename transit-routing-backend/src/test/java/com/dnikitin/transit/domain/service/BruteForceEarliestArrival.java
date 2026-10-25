package com.dnikitin.transit.domain.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reference for {@link RaptorRouter}: earliest arrival at the target using at most k trips, computed by
 * plain fixed-point relaxation over every trip and footpath (no route scanning, marking or pruning).
 * Same model as the router: boarding needs arrival <= departure at that stop, footpaths can be chained
 * and walking is allowed before the first and after the last trip.
 */
final class BruteForceEarliestArrival {

    static final long UNREACHED = Long.MAX_VALUE;

    private BruteForceEarliestArrival() {
    }

    /** Element k is the earliest arrival at the target with at most k trips, for k = 0..maxTrips. */
    static long[] byMaxTrips(TestNetwork network, int source, int target, int departure, int maxTrips) {
        Map<Integer, Integer> index = new HashMap<>();
        for (int stopId : network.stopIds) {
            index.put(stopId, index.size());
        }

        long[] arrival = new long[index.size()];
        Arrays.fill(arrival, UNREACHED);
        arrival[index.get(source)] = departure;
        walk(network, index, arrival);

        long[] result = new long[maxTrips + 1];
        result[0] = arrival[index.get(target)];

        for (int k = 1; k <= maxTrips; k++) {
            long[] next = arrival.clone();
            for (TestNetwork.Route route : network.routes) {
                for (TestNetwork.Trip trip : route.trips()) {
                    int boardAt = -1;
                    for (int i = 0; i < route.stopIds().length && boardAt < 0; i++) {
                        if (arrival[index.get(route.stopIds()[i])] <= trip.departures()[i]) {
                            boardAt = i;
                        }
                    }
                    if (boardAt < 0) {
                        continue;
                    }
                    for (int j = boardAt + 1; j < route.stopIds().length; j++) {
                        int stop = index.get(route.stopIds()[j]);
                        next[stop] = Math.min(next[stop], trip.arrivals()[j]);
                    }
                }
            }
            walk(network, index, next);
            arrival = next;
            result[k] = arrival[index.get(target)];
        }
        return result;
    }

    private static void walk(TestNetwork network, Map<Integer, Integer> index, long[] arrival) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TestNetwork.Footpath footpath : network.footpaths) {
                long from = arrival[index.get(footpath.fromStopId())];
                int to = index.get(footpath.toStopId());
                if (from != UNREACHED && from + footpath.durationSeconds() < arrival[to]) {
                    arrival[to] = from + footpath.durationSeconds();
                    changed = true;
                }
            }
        }
    }
}
