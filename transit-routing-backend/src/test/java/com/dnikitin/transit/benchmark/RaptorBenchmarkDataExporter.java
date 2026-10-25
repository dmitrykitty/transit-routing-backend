package com.dnikitin.transit.benchmark;

import com.dnikitin.transit.TransitRoutingBackendApplication;
import com.dnikitin.transit.application.service.RaptorDataBuilderService;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.StopRaptor;
import com.dnikitin.transit.domain.service.RaptorRouter;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the RAPTOR data set for one city and service date from the imported database (same code path
 * as the API), serializes it for the JMH benchmark, and writes a seeded set of random OD queries.
 * <p>
 * Args: cityId serviceDate queryCount seed (defaults: 1 2026-10-27 1000 42).
 * Requires the PostgreSQL database from docker-compose with GTFS data already imported.
 */
public final class RaptorBenchmarkDataExporter {

    private static final int FIRST_DEPARTURE_SECONDS = 6 * 3600;
    private static final int DEPARTURE_WINDOW_SECONDS = 16 * 3600;

    public static void main(String[] args) throws Exception {
        short cityId = Short.parseShort(arg(args, 0, "1"));
        LocalDate serviceDate = LocalDate.parse(arg(args, 1, "2026-10-27"));
        int queryCount = Integer.parseInt(arg(args, 2, "1000"));
        long seed = Long.parseLong(arg(args, 3, "42"));

        Path dir = BenchmarkData.benchDir();
        Files.createDirectories(dir);

        RaptorDataSet dataSet;
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TransitRoutingBackendApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "gtfs.initializer.enabled=false",
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.main.banner-mode=off"
                )
                .run()) {
            RaptorDataBuilderService builder = context.getBean(RaptorDataBuilderService.class);

            long start = System.nanoTime();
            dataSet = builder.buildForCity(cityId, serviceDate);
            System.out.printf("Data set build (cold, from PostgreSQL): %d ms%n", (System.nanoTime() - start) / 1_000_000);
        }

        printDataSetSize(dataSet, serviceDate);

        Path snapshot = dir.resolve(BenchmarkData.SNAPSHOT_FILE);
        BenchmarkData.writeSnapshot(dataSet, snapshot);
        System.out.printf("Snapshot: %s (%d bytes)%n", snapshot.toAbsolutePath(), Files.size(snapshot));

        List<BenchmarkData.Query> queries = randomQueries(dataSet, serviceDate, queryCount, seed);
        Path queriesFile = dir.resolve(BenchmarkData.QUERIES_FILE);
        BenchmarkData.writeQueries(queries, queriesFile);
        System.out.printf("Queries: %s (%d, seed %d, departures 06:00-21:59)%n", queriesFile.toAbsolutePath(), queries.size(), seed);

        printQueryOutcomes(dataSet, queries);
    }

    /**
     * Uniformly random source/target among stops served by at least one route on the service date,
     * departure time uniformly random (whole minutes) in [06:00, 22:00).
     */
    private static List<BenchmarkData.Query> randomQueries(RaptorDataSet dataSet, LocalDate serviceDate, int count, long seed) {
        List<Integer> servedStops = dataSet.stopsById().values().stream()
                .filter(stop -> !stop.routes().isEmpty())
                .map(StopRaptor::id)
                .sorted()
                .toList();

        Random random = new Random(seed);
        List<BenchmarkData.Query> queries = new ArrayList<>(count);
        while (queries.size() < count) {
            int source = servedStops.get(random.nextInt(servedStops.size()));
            int target = servedStops.get(random.nextInt(servedStops.size()));
            if (source == target) {
                continue;
            }
            int departureMinute = random.nextInt(DEPARTURE_WINDOW_SECONDS / 60);
            LocalTime departure = LocalTime.ofSecondOfDay(FIRST_DEPARTURE_SECONDS + departureMinute * 60L);
            queries.add(new BenchmarkData.Query(source, target, departure, serviceDate.toString()));
        }
        return queries;
    }

    private static void printDataSetSize(RaptorDataSet dataSet, LocalDate serviceDate) {
        long trips = 0;
        long stopEvents = 0;
        for (RouteRaptor route : dataSet.routesById().values()) {
            trips += route.trips().size();
            stopEvents += (long) route.trips().size() * route.stopIds().length;
        }
        long transfers = dataSet.stopsById().values().stream().mapToLong(stop -> stop.transfers().size()).sum();
        long servedStops = dataSet.stopsById().values().stream().filter(stop -> !stop.routes().isEmpty()).count();

        System.out.printf("Data set for %s: stops=%d (served=%d), route patterns=%d, trips=%d, stop events=%d, footpaths=%d%n",
                serviceDate, dataSet.stopsById().size(), servedStops, dataSet.routesById().size(), trips, stopEvents, transfers);
    }

    private static void printQueryOutcomes(RaptorDataSet dataSet, List<BenchmarkData.Query> queries) {
        RaptorRouter router = new RaptorRouter();
        int withJourney = 0;
        int maxTrips = 0;
        for (BenchmarkData.Query query : queries) {
            List<RaptorJourney> journeys = router.findJourneys(dataSet, query.sourceStopId(), query.targetStopId(), query.departureTime());
            if (!journeys.isEmpty()) {
                withJourney++;
                maxTrips = Math.max(maxTrips, journeys.getLast().tripCount());
            }
        }
        System.out.printf("Queries with at least one journey: %d/%d, max trips in a returned journey: %d%n",
                withJourney, queries.size(), maxTrips);
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }
}
