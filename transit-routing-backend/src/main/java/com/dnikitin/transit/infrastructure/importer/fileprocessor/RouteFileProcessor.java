package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.importer.util.ParsingUtil;
import com.dnikitin.transit.infrastructure.persistence.entity.*;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
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
public class RouteFileProcessor implements GtfsFileProcessor {
    private final RouteJpaRepository routeRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_ROUTE_SQL = """
            INSERT INTO route (
                route_id_ext,
                agency_id,
                route_number,
                name,
                vehicle_type,
                city_id
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (city_id, route_id_ext, vehicle_type) DO NOTHING
            """;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing routes.txt for city: {} from source: {}", city.getName(), source);

        Map<String, Long> agencyIdMap = fetchAgencyIdMap(city);
        VehicleType vehicleType = determineVehicleType(source);
        CsvParser parser = createCsvParser();

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                // GTFS routes.txt: route_id[0], agency_id[1], route_short_name[2], route_long_name[3]
                String agencyExtId = ParsingUtil.blankToNull(row[1]);
                Long agencyInternalId = (agencyExtId != null) ? agencyIdMap.get(agencyExtId) : null;

                batch.add(new Object[]{
                        row[0],                 // route_id_external
                        agencyInternalId,       // agency_id (Internal FK)
                        row[2],                 // route_number (short_name)
                        row[3],                 // name (long_name)
                        vehicleType.name(),     // vehicle_type (Enum as String)
                        city.getId()            // city_id
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(INSERT_ROUTE_SQL, batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            } catch (Exception e) {
                log.warn("Failed to map route row {}: {}", row[0], e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_ROUTE_SQL, batch);
            totalSaved += batch.size();
        }

        log.info("Imported {} routes for {} from {}", totalSaved, city.getName(), source);
    }

    private Map<String, Long> fetchAgencyIdMap(CityEntity city) {
        String sql = "SELECT agency_id_ext, id FROM agency WHERE city_id = ?";
        Map<String, Long> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getString(1), rs.getLong(2));
        }, city.getId());
        return map;
    }


    @Override
    public String getName() {
        return "routes.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 7;
    }

    @Override
    public void clear(CityEntity city) {
        log.info("Cleaning up routes for city: {}", city.getName());
        routeRepository.deleteRouteByCityBulk(city);
    }

    private VehicleType determineVehicleType(String source) {
        if (source.contains("_T")) return VehicleType.TRAM;
        return VehicleType.BUS;
    }
}
