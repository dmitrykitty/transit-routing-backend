package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StopTimeFileProcessor implements GtfsFileProcessor {
    private final StopTimeJpaRepository stopTimeRepository;
    private final TripJpaRepository tripRepository;
    private final StopJpaRepository stopRepository;
    private final EntityManager entityManager;

    private static final int BATCH_SIZE = 5000;

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Starting import of stop_times.txt for city: {} (Source: {})", cityName, source);

        //getting only mapped ID (External -> Internal)
        Map<String, Long> tripIdMap = tripRepository.findAllByCity(cityName).stream()
                .collect(Collectors.toMap(TripEntity::getTripIdExternal, TripEntity::getId));

        Map<String, Long> stopIdMap = stopRepository.findAllByCity(cityName).stream()
                .collect(Collectors.toMap(StopEntity::getStopIdExternal, StopEntity::getId));

        CsvParser parser = createCsvParser();
        List<StopTimeEntity> batch = new ArrayList<>(BATCH_SIZE);

        int totalSaved = 0;
        int skipped = 0;

        for (String[] row : parser.iterate(inputStream)) {
            Long tripInternalId = tripIdMap.get(row[0]);
            Long stopInternalId = stopIdMap.get(row[3]);

            if (tripInternalId == null || stopInternalId == null) {
                skipped++;
                continue;
            }

            try {
                batch.add(mapRowToEntity(row, tripInternalId, stopInternalId, cityName));

                if (batch.size() >= BATCH_SIZE) {
                    saveAndClear(batch);
                    totalSaved += batch.size();
                    batch.clear();

                    log.info("Partition import: imported {} stop_times", totalSaved);
                }
            } catch (Exception e) {
                skipped++;
                log.error("Error parsing row for trip {}: {}", row[0], e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            saveAndClear(batch);
            totalSaved += batch.size();
        }

        log.info("Imported {} stop_times for {} (skipped {})", totalSaved, cityName, skipped);
    }

    private void saveAndClear(List<StopTimeEntity> batch) {
        stopTimeRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public String getName() {
        return "stop_times.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 10;
    }


    @Override
    public void clear(String cityName) {
        log.info("Cleaning up stop times for city: {}", cityName);
        stopTimeRepository.deleteStopTimeByCityBulk(cityName);
    }

    private StopTimeEntity mapRowToEntity(String[] row, Long tripInternalId, Long stopInternalId, String cityName) {
        //getReference -> getProxy to reduce amount of selects to DB
        return StopTimeEntity.builder()
                .trip(entityManager.getReference(TripEntity.class, tripInternalId))
                .stop(entityManager.getReference(StopEntity.class, stopInternalId))
                .arrivalTime(parseGtfsTime(row[1]))
                .departureTime(parseGtfsTime(row[2]))
                .stopSequence(Integer.parseInt(row[4]))
                .stopHeadsign(blankToNull(row[5]))
                .pickupType(parseOrDefault(row[6]))
                .dropOffType(parseOrDefault(row[7]))
                .shapeDistTraveled(parseNullableDouble(row[8]))
                .timepoint(parseNullableInt(row[9]))
                .city(cityName)
                .build();
    }

    private LocalTime parseGtfsTime(String value) {
        String[] parts = value.trim().split(":");
        int hour = Integer.parseInt(parts[0]) % 24; // Obsługa GTFS 24h+
        return LocalTime.of(hour, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private Integer parseNullableInt(String value) {
        return (value == null || value.isBlank()) ? null : Integer.parseInt(value);
    }

    private int parseOrDefault(String value) {
        return (value == null || value.isBlank()) ? 0 : Integer.parseInt(value);
    }

    private Double parseNullableDouble(String value) {
        return (value == null || value.isBlank()) ? null : Double.parseDouble(value);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
