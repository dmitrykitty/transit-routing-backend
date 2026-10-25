package com.dnikitin.transit.benchmark;

import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.service.RaptorRouter;
import org.openjdk.jmh.annotations.*;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Algorithm-only latency of {@link RaptorRouter#findJourneys} on a real city snapshot.
 * Each invocation runs the next query from the OD file (cycling), so the reported distribution
 * is over the whole query set. Prepare inputs with {@link RaptorBenchmarkDataExporter}.
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 10)
@Measurement(iterations = 5, time = 20)
@Fork(value = 2, jvmArgsAppend = {"-Xms4g", "-Xmx4g"})
@State(Scope.Benchmark)
public class RaptorRouterBenchmark {

    @State(Scope.Benchmark)
    public static class Input {
        RaptorDataSet dataSet;
        int[] sources;
        int[] targets;
        LocalTime[] departures;

        @Setup(Level.Trial)
        public void load() throws Exception {
            dataSet = BenchmarkData.readSnapshot(BenchmarkData.benchDir().resolve(BenchmarkData.SNAPSHOT_FILE));
            List<BenchmarkData.Query> queries = BenchmarkData.readQueries(BenchmarkData.benchDir().resolve(BenchmarkData.QUERIES_FILE));
            sources = queries.stream().mapToInt(BenchmarkData.Query::sourceStopId).toArray();
            targets = queries.stream().mapToInt(BenchmarkData.Query::targetStopId).toArray();
            departures = queries.stream().map(BenchmarkData.Query::departureTime).toArray(LocalTime[]::new);
        }
    }

    @State(Scope.Thread)
    public static class Cursor {
        int next;
    }

    private final RaptorRouter router = new RaptorRouter();

    @Benchmark
    public List<RaptorJourney> findJourneys(Input input, Cursor cursor) {
        int i = cursor.next;
        cursor.next = (i + 1) % input.sources.length;
        return router.findJourneys(input.dataSet, input.sources[i], input.targets[i], input.departures[i]);
    }
}
