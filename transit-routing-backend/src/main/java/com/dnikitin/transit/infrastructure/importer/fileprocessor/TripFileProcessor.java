package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.importer.util.ParsingUtil;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.VehicleType;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripFileProcessor implements GtfsFileProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final TripJpaRepository tripRepository;
    private final BlockFileProcessor blockFileProcessor;

    private static final int BATCH_SIZE = 5000;

    private static final String IMPORT_TRIP_SQL =
            """
                    INSERT INTO trip (
                        trip_id_ext, route_id, calendar_id, city_id,
                        trip_headsign, direction_id, block_id, shift,
                        shape_id, wheelchair_accessible
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (trip_id_ext, city_id) DO NOTHING
                    """;


    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing trips.txt for city: {} (Source: {})", city.getName(), source);
        VehicleType currentVehicleType = determineVehicleType(source);

        // Pre-load Routes and Calendars to RAM for O(1) lookup
        Map<String, Long> routeIdMap = fetchRouteIdMap(city, currentVehicleType);
        Map<String, Long> calendarIdMap = fetchCalendarIdMap(city);

        CsvParser parser = createCsvParser();
        parser.beginParsing(inputStream);

        String[] headers = parser.getContext().headers();
        if (headers == null) {
            log.error("No headers found in trips.txt");
            return;
        }

        Map<String, Integer> h = IntStream.range(0, headers.length)
                .boxed()
                .collect(Collectors.toMap(
                        i -> headers[i].trim().toLowerCase(),
                        i -> i,
                        (existing, _) -> existing
                ));

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;
        int skipped = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                String tripIdExt = getVal(row, h, "trip_id");
                String routeIdExt = getVal(row, h, "route_id");
                String serviceIdExt = getVal(row, h, "service_id");

                Long routeInternalId = routeIdMap.get(routeIdExt);
                Long calendarInternalId = calendarIdMap.get(serviceIdExt);

                if (routeInternalId == null || calendarInternalId == null) {
                    skipped++;
                    continue;
                }

                String blockId = getVal(row, h, "block_id");
                String shift = (blockId != null && !blockId.isBlank())
                        ? blockFileProcessor.getShiftByBlockId(blockId, city)
                        : null;

                batch.add(new Object[]{
                        tripIdExt,                                               // trip_id_ext
                        routeInternalId,                                         // route_id
                        calendarInternalId,                                      // calendar_id
                        city.getId(),                                            // city_id
                        getVal(row, h, "trip_headsign"),                         // trip_headsign
                        ParsingUtil.parseNullableInt(getVal(row, h, "direction_id")), // direction_id
                        blockId,                                                 // block_id
                        shift,                                                   // shift
                        getVal(row, h, "shape_id"),                              // shape_id
                        ParsingUtil.parseNullableIntOrDefault(getVal(row, h, "wheelchair_accessible"), 0)
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(IMPORT_TRIP_SQL, batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            } catch (Exception e) {
                log.warn("Error parsing trip row for ID {}: {}", row[0], e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(IMPORT_TRIP_SQL, batch);
            totalSaved += batch.size();
        }

        log.info("Successfully imported {} trips for {} (skipped {} missing refs)", totalSaved, city.getName(), skipped);
    }

    private String getVal(String[] row, Map<String, Integer> headerMap, String colName) {
        Integer idx = headerMap.get(colName);
        if (idx == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.isBlank()) ? null : val.trim().replace("\"", "");
    }

    private Map<String, Long> fetchRouteIdMap(CityEntity city, VehicleType vehicleType) {
        String sql = "SELECT route_id_ext, id FROM route WHERE city_id = ? AND vehicle_type = ?";
        Map<String, Long> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getString(1), rs.getLong(2));
        }, city.getId(), vehicleType.name());
        return map;
    }

    private Map<String, Long> fetchCalendarIdMap(CityEntity city) {
        String sql = "SELECT service_id_ext, id FROM service_calendar WHERE city_id = ?";
        Map<String, Long> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getString(1), rs.getLong(2));
        }, city.getId());
        return map;
    }

    @Override
    public String getName() {
        return "trips.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 9;
    }

    @Override
    public void clear(CityEntity city) {
        log.info("Cleaning up trips for city: {}", city.getName());
        tripRepository.deleteTripByCityBulk(city);

    }

    private VehicleType determineVehicleType(String source) {
        if (source.contains("_T")) return VehicleType.TRAM;
        return VehicleType.BUS;
    }

}

