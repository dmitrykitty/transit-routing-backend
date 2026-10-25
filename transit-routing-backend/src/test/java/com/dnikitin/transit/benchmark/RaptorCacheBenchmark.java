package com.dnikitin.transit.benchmark;

import com.dnikitin.transit.TransitRoutingBackendApplication;
import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.service.RaptorDataBuilderService;
import com.dnikitin.transit.application.service.RaptorRoutingProperties;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Measures what caching the RAPTOR data set is worth: cold build time per service date
 * (and its DB-load breakdown), heap retained per cached date and the cost of an in-heap cache hit.
 * <p>
 * Args: cityId date1 [date2 ...] (defaults: 1 2026-10-27 2026-10-28 2026-10-29).
 * Requires PostgreSQL from docker-compose with GTFS data already imported.
 */
public final class RaptorCacheBenchmark {

    private static final int HIT_REPETITIONS = 100_000;

    public static void main(String[] args) {
        short cityId = Short.parseShort(args.length > 0 ? args[0] : "1");
        List<LocalDate> dates = args.length > 1
                ? Arrays.stream(args, 1, args.length).map(LocalDate::parse).toList()
                : List.of(LocalDate.parse("2026-10-27"), LocalDate.parse("2026-10-28"), LocalDate.parse("2026-10-29"));

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TransitRoutingBackendApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "gtfs.initializer.enabled=false",
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.main.banner-mode=off",
                        "logging.level.root=warn"
                )
                .run()) {
            RaptorDataBuilderService builder = context.getBean(RaptorDataBuilderService.class);

            System.out.println("## Cold builds (cache miss), one per service date");
            System.out.printf("Heap used after GC, empty cache: %d MB%n", heapUsedAfterGcMb());
            for (LocalDate date : dates) {
                long start = System.nanoTime();
                builder.buildForCity(cityId, date);
                long buildMs = (System.nanoTime() - start) / 1_000_000;
                System.out.printf("%s: build %d ms, heap used after GC with %s cached: %d MB%n",
                        date, buildMs, date, heapUsedAfterGcMb());
            }

            System.out.println("\n## In-heap cache hit");
            LocalDate hitDate = dates.getFirst();
            long checksum = 0;
            long start = System.nanoTime();
            for (int i = 0; i < HIT_REPETITIONS; i++) {
                checksum += builder.buildForCity(cityId, hitDate).routesById().size();
            }
            System.out.printf("buildForCity on a cached key: %.3f us/call (mean of %d, checksum %d)%n",
                    (System.nanoTime() - start) / 1_000.0 / HIT_REPETITIONS, HIT_REPETITIONS, checksum);

            System.out.println("\n## Cold-build breakdown (DB load through RaptorDataQueryPort, JVM warm)");
            printLoadBreakdown(context, cityId, hitDate);
        }
    }

    private static void printLoadBreakdown(ConfigurableApplicationContext context, short cityId, LocalDate date) {
        RaptorDataQueryPort port = context.getBean(RaptorDataQueryPort.class);
        RaptorRoutingProperties.Transfer transfer = context.getBean(RaptorRoutingProperties.class).getTransfer();

        long start = System.nanoTime();
        int stops = port.findStopsByCityId(cityId).size();
        System.out.printf("findStopsByCityId: %d ms (%d stops)%n", (System.nanoTime() - start) / 1_000_000, stops);

        start = System.nanoTime();
        int trips = port.findTripsByCityId(cityId, date).size();
        System.out.printf("findTripsByCityId(%s): %d ms (%d trips; loads all stop_times of the city)%n",
                date, (System.nanoTime() - start) / 1_000_000, trips);

        start = System.nanoTime();
        int transfers = port.findTransfersByCityId(cityId, transfer.getRadiusMeters(),
                transfer.getWalkingSpeedMetersPerSecond(), transfer.getMaxDurationSeconds()).size();
        System.out.printf("findTransfersByCityId: %d ms (%d footpaths)%n", (System.nanoTime() - start) / 1_000_000, transfers);
    }

    private static long heapUsedAfterGcMb() {
        System.gc();
        System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
}
