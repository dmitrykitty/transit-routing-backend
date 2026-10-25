package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopData;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopTimeData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTransferData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.TripRaptor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private RaptorRoutingProperties properties() {
        return new RaptorRoutingProperties();
    }

    private List<RaptorStopData> stops() {
        return List.of(
                new RaptorStopData(1, "Alpha", 50.0, 19.0),
                new RaptorStopData(2, "Bravo", 50.1, 19.1)
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
