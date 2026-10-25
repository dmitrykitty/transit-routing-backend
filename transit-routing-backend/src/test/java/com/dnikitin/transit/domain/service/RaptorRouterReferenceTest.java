package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares {@link RaptorRouter} with {@link BruteForceEarliestArrival} on seeded random networks.
 * Each property reports how many queries violate it and the first counterexample (seed, query, network).
 */
class RaptorRouterReferenceTest {

    private static final int NETWORKS = 500;
    private static final int QUERIES_PER_NETWORK = 20;

    private final RaptorRouter router = new RaptorRouter();

    @Test
    void earliestArrivalMatchesBruteForce() {
        assertHolds(query -> {
            long expected = query.reference()[query.reference().length - 1];
            if (expected == BruteForceEarliestArrival.UNREACHED) {
                return query.journeys().isEmpty() ? null : "target unreachable but router returned " + query.journeys();
            }
            long actual = query.journeys().stream().mapToLong(j -> j.arrivalTime().toSecondOfDay()).min().orElse(BruteForceEarliestArrival.UNREACHED);
            return actual == expected ? null : "earliest arrival " + time(actual) + ", brute force " + time(expected);
        });
    }

    @Test
    void everyReturnedJourneyIsExecutable() {
        assertHolds(query -> {
            for (RaptorJourney journey : query.journeys()) {
                List<String> problems = JourneyValidator.problems(query.network(), journey, query.source(), query.target(), query.departure());
                if (!problems.isEmpty()) {
                    return problems + " in " + journey;
                }
            }
            return null;
        });
    }

    @Test
    void bestArrivalForEachTripLimitIsFound() {
        assertHolds(query -> {
            long[] reference = query.reference();
            for (int k = 0; k < reference.length; k++) {
                boolean improves = reference[k] != BruteForceEarliestArrival.UNREACHED
                        && (k == 0 || reference[k] < reference[k - 1]);
                if (!improves) {
                    continue;
                }
                int limit = k;
                long best = reference[k];
                boolean found = query.journeys().stream()
                        .anyMatch(j -> j.tripCount() <= limit && j.arrivalTime().toSecondOfDay() <= best);
                if (!found) {
                    return "no journey with <= " + k + " trips arriving by " + time(best);
                }
            }
            return null;
        });
    }

    @Test
    void noReturnedJourneyIsDominated() {
        assertHolds(query -> {
            List<RaptorJourney> journeys = query.journeys();
            for (RaptorJourney a : journeys) {
                for (RaptorJourney b : journeys) {
                    if (a != b && b.tripCount() <= a.tripCount()
                            && b.arrivalTime().toSecondOfDay() <= a.arrivalTime().toSecondOfDay()) {
                        return summary(a) + " is dominated by " + summary(b);
                    }
                }
            }
            return null;
        });
    }

    @Test
    void randomNetworksIncludeOvertakingTrips() {
        int networksWithSplitRoutes = 0;
        for (int seed = 1; seed <= NETWORKS; seed++) {
            TestNetwork network = TestNetwork.random(new Random(seed));
            if (network.toDataSet().routesById().size() > network.routes.size()) {
                networksWithSplitRoutes++;
            }
        }

        assertTrue(networksWithSplitRoutes >= NETWORKS / 20,
                "only " + networksWithSplitRoutes + " of " + NETWORKS + " networks have overtaking trips");
    }

    private void assertHolds(Function<Query, String> property) {
        int checked = 0;
        int violations = 0;
        String first = null;

        for (int seed = 1; seed <= NETWORKS; seed++) {
            Random random = new Random(seed);
            TestNetwork network = TestNetwork.random(random);
            RaptorDataSet dataSet = network.toDataSet();

            for (int q = 0; q < QUERIES_PER_NETWORK; q++) {
                int source = network.stopIds.get(random.nextInt(network.stopIds.size()));
                int target = network.stopIds.get(random.nextInt(network.stopIds.size()));
                if (source == target) {
                    continue;
                }
                LocalTime departure = LocalTime.ofSecondOfDay(5 * 3600 + 1800 + random.nextInt(5 * 3600));
                List<RaptorJourney> journeys = router.findJourneys(dataSet, source, target, departure);
                long[] reference = BruteForceEarliestArrival.byMaxTrips(network, source, target,
                        departure.toSecondOfDay(), network.tripCount());

                checked++;
                String violation = property.apply(new Query(network, source, target, departure, journeys, reference));
                if (violation != null) {
                    violations++;
                    if (first == null) {
                        first = "seed " + seed + ", query " + source + " -> " + target + " at " + departure
                                + ": " + violation + "\n" + network;
                    }
                }
            }
        }

        assertTrue(violations == 0, violations + " of " + checked + " queries violate the property. First: " + first);
    }

    private static String summary(RaptorJourney journey) {
        return "(" + journey.tripCount() + " trips, arrives " + journey.arrivalTime() + ")";
    }

    private static String time(long seconds) {
        return seconds == BruteForceEarliestArrival.UNREACHED ? "unreached" : LocalTime.ofSecondOfDay(seconds).toString();
    }

    private record Query(TestNetwork network, int source, int target, LocalTime departure,
                         List<RaptorJourney> journeys, long[] reference) {
    }
}
