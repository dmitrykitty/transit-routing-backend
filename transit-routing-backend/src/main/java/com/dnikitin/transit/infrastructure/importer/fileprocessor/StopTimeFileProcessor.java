package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.importer.util.ParsingUtil;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StopTimeFileProcessor implements GtfsFileProcessor {
    private final StopTimeJpaRepository stopTimeRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 5000;

    private static final String IMPORT_STOP_TIME_SQL =
                    """
                        INSERT INTO stop_time (
                                               city_id, stop_id, trip_id,
                                               arrival_time, departure_time, drop_off_type, pickup_type,
                                               shape_dist_traveled, stop_headsign, stop_sequence, timepoint
                                               )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (trip_id, stop_sequence, city_id) DO NOTHING
                    """;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Starting import of stop_times.txt for city: {} (Source: {})", city.getName(), source);

        // getting only mapped ID (External -> Internal)
        // OPTIMIZATION: Fetching only IDs as Long/String pairs, not full Entities.
        // This avoids Hibernate overhead and OutOfMemoryError.
        Map<String, Long> tripIdMap = fetchInternalIdMap("trip", "trip_id_ext", city.getId());
        Map<String, Long> stopIdMap = fetchInternalIdMap("stop", "stop_id_ext", city.getId());

        CsvParser parser = createCsvParser();
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

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
                batch.add(new Object[]{
                        city.getId(),                                                   // city_id (FK)
                        stopInternalId,                                                 // stop_id (FK)
                        tripInternalId,                                                 // trip_id (FK)
                        ParsingUtil.parseGtfsTime(row[1]),                              // arrival_time (String "HH:mm:ss")
                        ParsingUtil.parseGtfsTime(row[2]),                              // departure_time
                        ParsingUtil.parseNullableIntOrDefault(row[7], 0),    // drop_off_type
                        ParsingUtil.parseNullableIntOrDefault(row[6], 0),    // pickup_type
                        ParsingUtil.parseNullableDouble(row[8]),                        // shape_dist_traveled
                        ParsingUtil.blankToNull(row[5]),                                // stop_headsign
                        Integer.parseInt(row[4]),                                       // stop_sequence
                        ParsingUtil.parseNullableInt(row[9])                            // timepoint
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(IMPORT_STOP_TIME_SQL, batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Progress: {} stop_times saved...", totalSaved);
                }
            } catch (Exception e) {
                skipped++;
                log.error("Row error (Trip: {}): {}", row[0], e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(IMPORT_STOP_TIME_SQL, batch);
            totalSaved += batch.size();
        }

        log.info("Import finished for {}. Saved: {}, Skipped: {}", city.getName(), totalSaved, skipped);
    }

    private Map<String, Long> fetchInternalIdMap(String tableName, String externalIdColumn, Short cityId) {
        String sql = "SELECT " + externalIdColumn + ", id FROM " + tableName + " WHERE city_id = ?";
        Map<String, Long> resultMap = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            resultMap.put(rs.getString(1), rs.getLong(2));
        }, cityId);
        return resultMap;
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
    public void clear(CityEntity city) {
        log.info("Cleaning up stop times for city: {}", city.getName());
        stopTimeRepository.deleteStopTimeByCityBulk(city);
    }
}
