package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.JourneyLegType;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static com.dnikitin.transit.domain.service.TestNetwork.trip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hand-made networks for behaviour the random tests may hit only rarely. Every returned journey is
 * also checked with {@link JourneyValidator}.
 */
class RaptorRouterScenarioTest {

    private static final int H = 3600;
    private static final int M = 60;

    private final RaptorRouter router = new RaptorRouter();

    @Test
    void switchesToEarlierTripAtLaterStopWhenReachedThereEarlier() {
        // From A only the 08:30 trip of route 1 can be boarded, but route 2 reaches B in time for the 08:00 trip.
        TestNetwork network = new TestNetwork().stops(1, 2, 3)
                .route(1, new int[]{1, 2, 3},
                        trip(11, 8 * H, 8 * H + 10 * M, 8 * H + 20 * M),
                        trip(12, 8 * H + 30 * M, 8 * H + 40 * M, 8 * H + 50 * M))
                .route(2, new int[]{1, 2}, trip(21, 8 * H + 6 * M, 8 * H + 8 * M));

        List<RaptorJourney> journeys = plan(network, 1, 3, LocalTime.of(8, 5));

        assertEquals(List.of(LocalTime.of(8, 50), LocalTime.of(8, 20)), arrivals(journeys));
        assertEquals(List.of(1, 2), tripCounts(journeys));
    }

    @Test
    void takesLaterTripThatOvertakesEarlierOneOnSameStops() {
        // The 08:10 express overtakes the 08:00 local on the same stop sequence.
        TestNetwork network = new TestNetwork().stops(1, 2, 3)
                .route(1, new int[]{1, 2, 3},
                        trip(11, 8 * H, 8 * H + 30 * M, 9 * H),
                        trip(12, 8 * H + 10 * M, 8 * H + 20 * M, 8 * H + 30 * M));

        List<RaptorJourney> journeys = plan(network, 1, 3, LocalTime.of(8, 0));

        assertEquals(List.of(LocalTime.of(8, 30)), arrivals(journeys));
        assertEquals(12, journeys.getFirst().legs().getFirst().tripId());
    }

    @Test
    void chainsFootpathsToReachTargetWithoutTrips() {
        TestNetwork network = new TestNetwork().stops(1, 2, 3)
                .footpath(1, 2, 120)
                .footpath(2, 3, 180);

        List<RaptorJourney> journeys = plan(network, 1, 3, LocalTime.of(9, 0));

        assertEquals(1, journeys.size());
        assertEquals(LocalTime.of(9, 5), journeys.getFirst().arrivalTime());
        assertEquals(0, journeys.getFirst().tripCount());
        assertEquals(List.of(JourneyLegType.TRANSFER, JourneyLegType.TRANSFER),
                journeys.getFirst().legs().stream().map(leg -> leg.type()).toList());
    }

    @Test
    void walksToTargetAfterLastTrip() {
        TestNetwork network = new TestNetwork().stops(1, 2, 3)
                .route(1, new int[]{1, 2}, trip(11, 8 * H, 8 * H + 15 * M))
                .footpath(2, 3, 240);

        List<RaptorJourney> journeys = plan(network, 1, 3, LocalTime.of(7, 50));

        assertEquals(List.of(LocalTime.of(8, 19)), arrivals(journeys));
        assertEquals(List.of(1), tripCounts(journeys));
    }

    @Test
    void walkThenRideIsNotReportedNextToADominatedRideOnlyJourney() {
        // Direct trip from 1 arrives 09:00; walking one minute to 2 and riding route 2 arrives 08:30.
        // Both use one trip, so only the 08:30 journey is Pareto-optimal.
        TestNetwork network = new TestNetwork().stops(1, 2, 3)
                .route(1, new int[]{1, 3}, trip(11, 8 * H, 9 * H))
                .route(2, new int[]{2, 3}, trip(21, 8 * H + 5 * M, 8 * H + 30 * M))
                .footpath(1, 2, 60);

        List<RaptorJourney> journeys = plan(network, 1, 3, LocalTime.of(7, 55));

        assertEquals(List.of(LocalTime.of(8, 30)), arrivals(journeys));
        assertEquals(List.of(1), tripCounts(journeys));
    }

    @Test
    void walkingFromSourceDoesNotCostAnExtraTransferInTheResult() {
        // Walk 1 -> 2, ride route 1 to 3, ride route 2 to 4: two trips, one transfer.
        TestNetwork network = new TestNetwork().stops(1, 2, 3, 4)
                .route(1, new int[]{2, 3}, trip(11, 8 * H, 8 * H + 10 * M))
                .route(2, new int[]{3, 4}, trip(21, 8 * H + 15 * M, 8 * H + 25 * M))
                .footpath(1, 2, 120);

        List<RaptorJourney> journeys = plan(network, 1, 4, LocalTime.of(7, 50));

        assertEquals(1, journeys.size());
        assertEquals(LocalTime.of(8, 25), journeys.getFirst().arrivalTime());
        assertEquals(2, journeys.getFirst().tripCount());
        assertEquals(1, journeys.getFirst().transferCount());
    }

    private List<RaptorJourney> plan(TestNetwork network, int source, int target, LocalTime departure) {
        List<RaptorJourney> journeys = router.findJourneys(network.toDataSet(), source, target, departure);
        for (RaptorJourney journey : journeys) {
            List<String> problems = JourneyValidator.problems(network, journey, source, target, departure);
            assertTrue(problems.isEmpty(), problems + " in " + journey);
        }
        return journeys;
    }

    private static List<LocalTime> arrivals(List<RaptorJourney> journeys) {
        return journeys.stream().map(RaptorJourney::arrivalTime).toList();
    }

    private static List<Integer> tripCounts(List<RaptorJourney> journeys) {
        return journeys.stream().map(RaptorJourney::tripCount).toList();
    }
}
