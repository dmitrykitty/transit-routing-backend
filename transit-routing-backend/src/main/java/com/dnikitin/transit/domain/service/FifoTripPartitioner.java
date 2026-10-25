package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.TripRaptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splits the trips of one stop pattern into groups in which no trip overtakes another: within a group,
 * trips sorted by first departure also have non-decreasing arrival and departure times at every stop.
 * {@link RaptorRouter} relies on this to find the earliest catchable trip of a route by binary search.
 * Patterns without overtaking (all of them in the Krakow feed for a single service date) stay one group.
 */
public final class FifoTripPartitioner {

    private static final Comparator<TripRaptor> BY_FIRST_DEPARTURE = Comparator
            .comparingInt((TripRaptor trip) -> trip.departureTimes()[0])
            .thenComparingInt(trip -> trip.arrivalTimes()[trip.arrivalTimes().length - 1]);

    private FifoTripPartitioner() {
    }

    public static List<List<TripRaptor>> partition(List<TripRaptor> trips) {
        List<TripRaptor> sorted = new ArrayList<>(trips);
        sorted.sort(BY_FIRST_DEPARTURE);

        List<List<TripRaptor>> groups = new ArrayList<>();
        for (TripRaptor trip : sorted) {
            List<TripRaptor> target = null;
            for (List<TripRaptor> group : groups) {
                if (neverEarlierThan(trip, group.getLast())) {
                    target = group;
                    break;
                }
            }
            if (target == null) {
                target = new ArrayList<>();
                groups.add(target);
            }
            target.add(trip);
        }

        return groups.stream().map(List::copyOf).toList();
    }

    private static boolean neverEarlierThan(TripRaptor later, TripRaptor earlier) {
        for (int i = 0; i < later.departureTimes().length; i++) {
            if (later.departureTimes()[i] < earlier.departureTimes()[i]
                    || later.arrivalTimes()[i] < earlier.arrivalTimes()[i]) {
                return false;
            }
        }
        return true;
    }
}
