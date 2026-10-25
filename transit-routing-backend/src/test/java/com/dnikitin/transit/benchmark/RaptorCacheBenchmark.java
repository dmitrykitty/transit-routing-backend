package com.dnikitin.transit.benchmark;

import com.dnikitin.transit.TransitRoutingBackendApplication;
import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.service.RaptorDataBuilderService;
import com.dnikitin.transit.application.service.RaptorRoutingProperties;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Measures what caching the RAPTOR data set is worth and what Redis could add:
 * cold build time per service date (and its DB-load breakdown), heap retained per cached date,
 * the cost of an in-heap cache hit, and storing/loading a serialized snapshot through Redis.
 * <p>
 * Args: cityId redisRepetitions date1 [date2 ...] (defaults: 1 20 2026-10-27 2026-10-28 2026-10-29).
 * Requires PostgreSQL (imported data) and Redis from docker-compose.
 */
public final class RaptorCacheBenchmark {

    private static final int HIT_REPETITIONS = 100_000;

    public static void main(String[] args) throws Exception {
        short cityId = Short.parseShort(args.length > 0 ? args[0] : "1");
        int redisRepetitions = Integer.parseInt(args.length > 1 ? args[1] : "20");
        List<LocalDate> dates = args.length > 2
                ? Arrays.stream(args, 2, args.length).map(LocalDate::parse).toList()
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
            RaptorDataSet first = null;
            for (LocalDate date : dates) {
                long start = System.nanoTime();
                RaptorDataSet dataSet = builder.buildForCity(cityId, date);
                long buildMs = (System.nanoTime() - start) / 1_000_000;
                if (first == null) {
                    first = dataSet;
                }
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

            System.out.println("\n## Serialized snapshot through Redis");
            printRedisSnapshot(context.getBean(RedisConnectionFactory.class), first, cityId, hitDate, redisRepetitions);
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

    private static void printRedisSnapshot(RedisConnectionFactory connectionFactory, RaptorDataSet dataSet,
                                           short cityId, LocalDate date, int repetitions) throws Exception {
        byte[] key = ("bench:raptor:snapshot:" + cityId + ":" + date).getBytes(StandardCharsets.UTF_8);

        long start = System.nanoTime();
        byte[] bytes = serialize(dataSet);
        System.out.printf("Java serialization: %d ms, %d bytes%n", (System.nanoTime() - start) / 1_000_000, bytes.length);

        List<Double> setMs = new ArrayList<>();
        List<Double> getMs = new ArrayList<>();
        List<Double> deserializeMs = new ArrayList<>();
        try (RedisConnection connection = connectionFactory.getConnection()) {
            for (int i = 0; i < repetitions; i++) {
                long t0 = System.nanoTime();
                connection.stringCommands().set(key, bytes);
                long t1 = System.nanoTime();
                byte[] loaded = connection.stringCommands().get(key);
                long t2 = System.nanoTime();
                RaptorDataSet restored = deserialize(loaded);
                long t3 = System.nanoTime();
                if (restored.routesById().size() != dataSet.routesById().size()) {
                    throw new IllegalStateException("Snapshot round trip changed the data set");
                }
                setMs.add((t1 - t0) / 1e6);
                getMs.add((t2 - t1) / 1e6);
                deserializeMs.add((t3 - t2) / 1e6);
            }
            connection.keyCommands().del(key);
        }

        System.out.printf("Redis SET  (%d runs): %s%n", repetitions, summary(setMs));
        System.out.printf("Redis GET  (%d runs): %s%n", repetitions, summary(getMs));
        System.out.printf("Deserialize(%d runs): %s%n", repetitions, summary(deserializeMs));
    }

    private static String summary(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        return String.format("min %.1f ms, median %.1f ms, max %.1f ms",
                sorted.getFirst(), sorted.get(sorted.size() / 2), sorted.getLast());
    }

    private static byte[] serialize(RaptorDataSet dataSet) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(dataSet);
        }
        return buffer.toByteArray();
    }

    private static RaptorDataSet deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (RaptorDataSet) in.readObject();
        }
    }

    private static long heapUsedAfterGcMb() {
        System.gc();
        System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
}
