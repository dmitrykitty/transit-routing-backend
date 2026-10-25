package com.dnikitin.transit.benchmark;

import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Files shared by the benchmark tools: a serialized RAPTOR data set for one city and service date,
 * and a CSV of origin-destination queries (sourceStopId,targetStopId,departureTime,serviceDate).
 */
final class BenchmarkData {

    static final String SNAPSHOT_FILE = "raptor-dataset.ser";
    static final String QUERIES_FILE = "od-pairs.csv";

    private BenchmarkData() {
    }

    static Path benchDir() {
        return Path.of(System.getProperty("raptor.bench.dir", "target/bench"));
    }

    static void writeSnapshot(RaptorDataSet dataSet, Path file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeObject(dataSet);
        }
    }

    static RaptorDataSet readSnapshot(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            return (RaptorDataSet) in.readObject();
        }
    }

    static void writeQueries(List<Query> queries, Path file) throws IOException {
        List<String> lines = new ArrayList<>(queries.size() + 1);
        lines.add("sourceStopId,targetStopId,departureTime,serviceDate");
        for (Query query : queries) {
            lines.add(query.sourceStopId() + "," + query.targetStopId() + "," + query.departureTime() + "," + query.serviceDate());
        }
        Files.write(file, lines);
    }

    static List<Query> readQueries(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(line -> line.split(","))
                .map(parts -> new Query(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        LocalTime.parse(parts[2]),
                        parts[3]
                ))
                .toList();
    }

    record Query(int sourceStopId, int targetStopId, LocalTime departureTime, String serviceDate) {
    }
}
