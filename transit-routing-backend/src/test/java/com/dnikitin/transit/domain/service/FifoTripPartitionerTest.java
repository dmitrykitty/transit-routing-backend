package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.TripRaptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FifoTripPartitionerTest {

    @Test
    void keepsTripsWithoutOvertakingInOneGroupSortedByDeparture() {
        TripRaptor late = trip(2, 900, 1500, 2100);
        TripRaptor early = trip(1, 0, 600, 1200);

        List<List<TripRaptor>> groups = FifoTripPartitioner.partition(List.of(late, early));

        assertEquals(List.of(List.of(early, late)), groups);
    }

    @Test
    void separatesTripThatOvertakesAnEarlierOne() {
        TripRaptor local = trip(1, 0, 1200, 3600);
        TripRaptor express = trip(2, 600, 900, 1800);
        TripRaptor nextLocal = trip(3, 1800, 3000, 5400);

        List<List<TripRaptor>> groups = FifoTripPartitioner.partition(List.of(express, nextLocal, local));

        assertEquals(List.of(List.of(local, nextLocal), List.of(express)), groups);
    }

    @Test
    void treatsEqualTimesAsNotOvertaking() {
        TripRaptor first = trip(1, 0, 600);
        TripRaptor second = trip(2, 0, 600);

        assertEquals(1, FifoTripPartitioner.partition(List.of(first, second)).size());
    }

    private static TripRaptor trip(int id, int... times) {
        return new TripRaptor(id, times.clone(), times.clone());
    }
}
