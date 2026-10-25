package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopData;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopTimeData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTransferData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.model.raptor.TripRaptor;
import com.dnikitin.transit.domain.service.RaptorRouter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RaptorDataBuilderServiceTest {

    @Test
    void cachesDatasetByCityAndServiceDate() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        LocalDate serviceDate = LocalDate.of(2026, 4, 28);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());

        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, serviceDate)).thenReturn(List.of(trip()));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        RaptorDataSet first = service.buildForCity((short) 1, serviceDate);
        RaptorDataSet second = service.buildForCity((short) 1, serviceDate);

        assertSame(first, second);
        verify(queryPort, times(1)).findStopsByCityId((short) 1);
        verify(queryPort, times(1)).findTripsByCityId((short) 1, serviceDate);
        verify(queryPort, times(1)).findTransfersByCityId((short) 1, 150.0, 1.4, 300);
    }

    @Test
    void invalidatesAllCachedDatasetsForCity() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());

        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, null)).thenReturn(List.of(trip()));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        RaptorDataSet first = service.buildForCity((short) 1);
        service.invalidateCity((short) 1);
        RaptorDataSet second = service.buildForCity((short) 1);

        verify(queryPort, times(2)).findStopsByCityId((short) 1);
        assertEquals(first.stopsById().keySet(), second.stopsById().keySet());
    }

    @Test
    void includesGeneratedTransfersInStops() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());

        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, null)).thenReturn(List.of());
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300))
                .thenReturn(List.of(new RaptorTransferData(1, 2, 90)));

        RaptorDataSet dataSet = service.buildForCity((short) 1);

        assertEquals(1, dataSet.stopsById().get(1).transfers().size());
        assertEquals(2, dataSet.stopsById().get(1).transfers().getFirst().destinationStopId());
        assertEquals(90, dataSet.stopsById().get(1).transfers().getFirst().durationInSeconds());
    }

    @Test
    void keepsTripTimesAfterMidnightOfServiceDay() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());

        // 23:50 -> 24:10 and a later trip 24:05 -> 24:25 (GTFS times past midnight of the service day).
        RaptorTripData lateTrip = new RaptorTripData(11, 100, 0, "Bravo", List.of(
                new RaptorStopTimeData(1, "Alpha", 86_700, 86_700, 1),
                new RaptorStopTimeData(2, "Bravo", 87_900, 87_900, 2)
        ));
        RaptorTripData crossingTrip = new RaptorTripData(10, 100, 0, "Bravo", List.of(
                new RaptorStopTimeData(1, "Alpha", 85_800, 85_800, 1),
                new RaptorStopTimeData(2, "Bravo", 87_000, 87_000, 2)
        ));
        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, null)).thenReturn(List.of(lateTrip, crossingTrip));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        RouteRaptor route = service.buildForCity((short) 1).routesById().values().iterator().next();

        List<TripRaptor> trips = route.trips();
        assertEquals(List.of(10, 11), trips.stream().map(TripRaptor::id).toList());
        assertArrayEquals(new int[]{85_800, 87_000}, trips.get(0).arrivalTimes());
        assertArrayEquals(new int[]{86_700, 87_900}, trips.get(1).departureTimes());
    }

    @Test
    void addsPreviousServiceDayTripsRunningPastMidnightShiftedByOneDay() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());
        LocalDate wednesday = LocalDate.of(2026, 4, 29);

        // Tuesday's 24:30 -> 24:40 is Wednesday 00:30 -> 00:40; Tuesday's 08:00 trip ends before midnight.
        RaptorTripData tuesdayNightTrip = new RaptorTripData(20, 100, 0, "Bravo", List.of(
                new RaptorStopTimeData(1, "Alpha", 88_200, 88_200, 1),
                new RaptorStopTimeData(2, "Bravo", 88_800, 88_800, 2)
        ));
        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, wednesday)).thenReturn(List.of(trip()));
        when(queryPort.findTripsByCityId((short) 1, wednesday.minusDays(1)))
                .thenReturn(List.of(trip(), tuesdayNightTrip));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        RaptorDataSet dataSet = service.buildForCity((short) 1, wednesday);

        // Same stops, route, direction and headsign: one pattern, ordered by first departure.
        assertEquals(1, dataSet.routesById().size());
        List<TripRaptor> trips = dataSet.routesById().values().iterator().next().trips();
        assertEquals(List.of(20, 10), trips.stream().map(TripRaptor::id).toList());
        assertArrayEquals(new int[]{1_800, 2_400}, trips.get(0).departureTimes());

        List<RaptorJourney> journeys = new RaptorRouter().findJourneys(dataSet, 1, 2, LocalTime.of(0, 20));
        assertEquals(LocalTime.of(0, 40), journeys.getLast().arrivalTime());
        assertEquals(20, journeys.getLast().legs().getFirst().tripId());
    }

    @Test
    void keepsPreviousDayTripCrossingMidnightFromItsFirstStopAfterMidnight() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());
        LocalDate wednesday = LocalDate.of(2026, 4, 29);

        // Tuesday 23:50 Alpha -> 24:05 Bravo -> 24:15 Charlie: only Bravo -> Charlie runs on Wednesday.
        RaptorTripData crossingTrip = new RaptorTripData(30, 100, 0, "Charlie", List.of(
                new RaptorStopTimeData(1, "Alpha", 85_800, 85_800, 1),
                new RaptorStopTimeData(2, "Bravo", 86_700, 86_760, 2),
                new RaptorStopTimeData(3, "Charlie", 87_300, 87_300, 3)
        ));
        // Tuesday 23:40 -> 24:01: no departure after midnight, nothing left to ride on Wednesday.
        RaptorTripData arrivingTrip = new RaptorTripData(31, 100, 0, "Bravo", List.of(
                new RaptorStopTimeData(1, "Alpha", 85_200, 85_200, 1),
                new RaptorStopTimeData(2, "Bravo", 86_460, 86_460, 2)
        ));
        when(queryPort.findStopsByCityId((short) 1)).thenReturn(threeStops());
        when(queryPort.findTripsByCityId((short) 1, wednesday)).thenReturn(List.of());
        when(queryPort.findTripsByCityId((short) 1, wednesday.minusDays(1)))
                .thenReturn(List.of(crossingTrip, arrivingTrip));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        RaptorDataSet dataSet = service.buildForCity((short) 1, wednesday);

        assertEquals(1, dataSet.routesById().size());
        RouteRaptor route = dataSet.routesById().values().iterator().next();
        assertArrayEquals(new int[]{2, 3}, route.stopIds());
        assertEquals(List.of(30), route.trips().stream().map(TripRaptor::id).toList());
        assertArrayEquals(new int[]{300, 900}, route.trips().getFirst().arrivalTimes());
        assertArrayEquals(new int[]{360, 900}, route.trips().getFirst().departureTimes());
        assertTrue(dataSet.stopsById().get(1).routes().isEmpty());

        List<RaptorJourney> journeys = new RaptorRouter().findJourneys(dataSet, 2, 3, LocalTime.of(0, 0));
        assertEquals(LocalTime.of(0, 15), journeys.getLast().arrivalTime());
    }

    @Test
    void doesNotAddPreviousDayTripsWithoutServiceDate() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());

        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, null)).thenReturn(List.of(trip()));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        service.buildForCity((short) 1);

        verify(queryPort, times(1)).findTripsByCityId(eq((short) 1), any());
    }

    @Test
    void splitsPatternWhoseTripsOvertakeIntoSeparateRoutes() {
        RaptorDataQueryPort queryPort = mock(RaptorDataQueryPort.class);
        RaptorDataBuilderService service = new RaptorDataBuilderService(queryPort, properties());

        // Same stops, but the 08:05 express arrives before the 08:00 local.
        RaptorTripData local = new RaptorTripData(10, 100, 0, "Bravo", List.of(
                new RaptorStopTimeData(1, "Alpha", 8 * 3600, 8 * 3600, 1),
                new RaptorStopTimeData(2, "Bravo", 8 * 3600 + 1800, 8 * 3600 + 1800, 2)
        ));
        RaptorTripData express = new RaptorTripData(11, 100, 0, "Bravo", List.of(
                new RaptorStopTimeData(1, "Alpha", 8 * 3600 + 300, 8 * 3600 + 300, 1),
                new RaptorStopTimeData(2, "Bravo", 8 * 3600 + 900, 8 * 3600 + 900, 2)
        ));
        when(queryPort.findStopsByCityId((short) 1)).thenReturn(stops());
        when(queryPort.findTripsByCityId((short) 1, null)).thenReturn(List.of(express, local));
        when(queryPort.findTransfersByCityId((short) 1, 150.0, 1.4, 300)).thenReturn(List.of());

        RaptorDataSet dataSet = service.buildForCity((short) 1);

        assertEquals(2, dataSet.routesById().size());
        assertEquals(List.of(List.of(10), List.of(11)), dataSet.routesById().values().stream()
                .map(route -> route.trips().stream().map(TripRaptor::id).toList())
                .toList());
        assertEquals(2, dataSet.stopsById().get(1).routes().size());
    }

    private RaptorRoutingProperties properties() {
        return new RaptorRoutingProperties();
    }

    private List<RaptorStopData> stops() {
        return List.of(
                new RaptorStopData(1, "Alpha", 50.0, 19.0),
                new RaptorStopData(2, "Bravo", 50.1, 19.1)
        );
    }

    private List<RaptorStopData> threeStops() {
        return List.of(
                new RaptorStopData(1, "Alpha", 50.0, 19.0),
                new RaptorStopData(2, "Bravo", 50.1, 19.1),
                new RaptorStopData(3, "Charlie", 50.2, 19.2)
        );
    }

    private RaptorTripData trip() {
        return new RaptorTripData(
                10,
                100,
                0,
                "Bravo",
                List.of(
                        new RaptorStopTimeData(1, "Alpha", 8 * 3600, 8 * 3600, 1),
                        new RaptorStopTimeData(2, "Bravo", 8 * 3600 + 600, 8 * 3600 + 600, 2)
                )
        );
    }
}
