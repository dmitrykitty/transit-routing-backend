package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.*;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaptorRouterTest {

    private final RaptorRouter raptorRouter = new RaptorRouter();

    @Test
    void findsDirectJourneyOnSingleRoute() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(new RouteAtStopRaptor(10, 0)), List.of()),
                        2, stop(2, "Bravo", List.of(new RouteAtStopRaptor(10, 1)), List.of()),
                        3, stop(3, "Charlie", List.of(new RouteAtStopRaptor(10, 2)), List.of())
                ),
                Map.of(
                        10, route(10, 1001L, "Downtown", new int[]{1, 2, 3}, List.of(
                                trip(501, new int[]{8 * 3600, 8 * 3600 + 600, 8 * 3600 + 1200})
                        ))
                ),
                List.of(10)
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 3, LocalTime.of(7, 55));

        assertEquals(1, journeys.size());

        RaptorJourney journey = journeys.getFirst();
        assertEquals(LocalTime.of(8, 20), journey.arrivalTime());
        assertEquals(1, journey.tripCount());
        assertEquals(0, journey.transferCount());
        assertEquals(1, journey.legs().size());
        assertEquals(JourneyLegType.RIDE, journey.legs().getFirst().type());
        assertEquals(1, journey.legs().getFirst().fromStopId());
        assertEquals(3, journey.legs().getFirst().toStopId());
    }

    @Test
    void returnsParetoJourneysAcrossRounds() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(
                                new RouteAtStopRaptor(10, 0),
                                new RouteAtStopRaptor(30, 0)
                        ), List.of()),
                        2, stop(2, "Bravo", List.of(
                                new RouteAtStopRaptor(10, 1),
                                new RouteAtStopRaptor(20, 0)
                        ), List.of()),
                        3, stop(3, "Charlie", List.of(
                                new RouteAtStopRaptor(20, 1),
                                new RouteAtStopRaptor(30, 1)
                        ), List.of())
                ),
                Map.of(
                        10, route(10, 1001L, "Bravo", new int[]{1, 2}, List.of(
                                trip(501, new int[]{8 * 3600, 8 * 3600 + 600})
                        )),
                        20, route(20, 1002L, "Charlie", new int[]{2, 3}, List.of(
                                trip(502, new int[]{8 * 3600 + 720, 8 * 3600 + 1200})
                        )),
                        30, route(30, 1003L, "Charlie", new int[]{1, 3}, List.of(
                                trip(503, new int[]{8 * 3600 + 1800, 8 * 3600 + 3000})
                        ))
                ),
                List.of(10, 20, 30)
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 3, LocalTime.of(7, 55));

        assertEquals(2, journeys.size());

        RaptorJourney directJourney = journeys.get(0);
        assertEquals(LocalTime.of(8, 50), directJourney.arrivalTime());
        assertEquals(1, directJourney.tripCount());
        assertEquals(0, directJourney.transferCount());

        RaptorJourney transferJourney = journeys.get(1);
        assertEquals(LocalTime.of(8, 20), transferJourney.arrivalTime());
        assertEquals(2, transferJourney.tripCount());
        assertEquals(1, transferJourney.transferCount());
        assertEquals(2, transferJourney.legs().size());
        assertTrue(transferJourney.legs().stream().allMatch(leg -> leg.type() == JourneyLegType.RIDE));
    }

    @Test
    void usesInitialTransferBeforeBoardingFirstTrip() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(), List.of(new TransferRaptor(2, 300))),
                        2, stop(2, "Bravo", List.of(new RouteAtStopRaptor(10, 0)), List.of()),
                        3, stop(3, "Charlie", List.of(new RouteAtStopRaptor(10, 1)), List.of())
                ),
                Map.of(
                        10, route(10, 1001L, "Charlie", new int[]{2, 3}, List.of(
                                trip(501, new int[]{8 * 3600 + 420, 8 * 3600 + 900})
                        ))
                ),
                List.of(10)
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 3, LocalTime.of(8, 0));

        assertEquals(1, journeys.size());

        RaptorJourney journey = journeys.getFirst();
        assertEquals(LocalTime.of(8, 15), journey.arrivalTime());
        assertEquals(2, journey.legs().size());
        assertEquals(JourneyLegType.TRANSFER, journey.legs().get(0).type());
        assertEquals(LocalTime.of(8, 0), journey.legs().get(0).departureTime());
        assertEquals(LocalTime.of(8, 5), journey.legs().get(0).arrivalTime());
        assertEquals(JourneyLegType.RIDE, journey.legs().get(1).type());
        assertEquals(LocalTime.of(8, 7), journey.legs().get(1).departureTime());
        assertEquals(LocalTime.of(8, 15), journey.legs().get(1).arrivalTime());
    }

    @Test
    void reprocessesTransferStopWhenArrivalImprovesAfterItWasQueued() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(), List.of(
                                new TransferRaptor(2, 100),
                                new TransferRaptor(3, 10)
                        )),
                        2, stop(2, "Bravo", List.of(), List.of(new TransferRaptor(4, 10))),
                        3, stop(3, "Charlie", List.of(), List.of(new TransferRaptor(2, 10))),
                        4, stop(4, "Delta", List.of(), List.of())
                ),
                Map.of(),
                List.of()
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 4, LocalTime.of(8, 0));

        assertEquals(1, journeys.size());
        assertEquals(LocalTime.of(8, 0, 30), journeys.getFirst().arrivalTime());
        assertEquals(3, journeys.getFirst().legs().size());
        assertEquals(3, journeys.getFirst().legs().get(0).toStopId());
        assertEquals(2, journeys.getFirst().legs().get(1).toStopId());
        assertEquals(4, journeys.getFirst().legs().get(2).toStopId());
    }

    @Test
    void wrapsAfterMidnightTransferArrivalsToLocalTime() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(), List.of(new TransferRaptor(2, 120))),
                        2, stop(2, "Bravo", List.of(), List.of())
                ),
                Map.of(),
                List.of()
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 2, LocalTime.of(23, 59));

        assertEquals(1, journeys.size());
        assertEquals(LocalTime.of(0, 1), journeys.getFirst().arrivalTime());
        assertEquals(LocalTime.of(23, 59), journeys.getFirst().legs().getFirst().departureTime());
        assertEquals(LocalTime.of(0, 1), journeys.getFirst().legs().getFirst().arrivalTime());
    }

    @Test
    void returnsEmptyJourneyWhenSourceEqualsTarget() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(1, stop(1, "Alpha", List.of(), List.of())),
                Map.of(),
                List.of()
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 1, LocalTime.of(8, 0));

        assertEquals(1, journeys.size());
        assertEquals(LocalTime.of(8, 0), journeys.getFirst().arrivalTime());
        assertEquals(0, journeys.getFirst().tripCount());
        assertTrue(journeys.getFirst().legs().isEmpty());
    }

    @Test
    void returnsNoJourneyWhenNoTripCanBeCaught() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(new RouteAtStopRaptor(10, 0)), List.of()),
                        2, stop(2, "Bravo", List.of(new RouteAtStopRaptor(10, 1)), List.of())
                ),
                Map.of(
                        10, route(10, 1001L, "Bravo", new int[]{1, 2}, List.of(
                                trip(501, new int[]{8 * 3600, 8 * 3600 + 600})
                        ))
                ),
                List.of(10)
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 2, LocalTime.of(8, 1));

        assertTrue(journeys.isEmpty());
    }

    @Test
    void returnsNoJourneyWhenTargetIsUnreachable() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(
                        1, stop(1, "Alpha", List.of(), List.of()),
                        2, stop(2, "Bravo", List.of(), List.of())
                ),
                Map.of(),
                List.of()
        );

        List<RaptorJourney> journeys = raptorRouter.findJourneys(dataSet, 1, 2, LocalTime.of(8, 0));

        assertTrue(journeys.isEmpty());
    }

    @Test
    void rejectsUnknownStops() {
        RaptorDataSet dataSet = new RaptorDataSet(
                Map.of(1, stop(1, "Alpha", List.of(), List.of())),
                Map.of(),
                List.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> raptorRouter.findJourneys(dataSet, 1, 2, LocalTime.of(8, 0))
        );
    }

    private StopRaptor stop(
            int id,
            String name,
            List<RouteAtStopRaptor> routes,
            List<TransferRaptor> transfers
    ) {
        return new StopRaptor(id, name, routes, transfers);
    }

    private RouteRaptor route(
            int id,
            Long sourceRouteId,
            String headsign,
            int[] stopIds,
            List<TripRaptor> trips
    ) {
        return new RouteRaptor(id, sourceRouteId, 0, headsign, stopIds, trips);
    }

    private TripRaptor trip(int id, int[] times) {
        return new TripRaptor(id, times, times);
    }
}
