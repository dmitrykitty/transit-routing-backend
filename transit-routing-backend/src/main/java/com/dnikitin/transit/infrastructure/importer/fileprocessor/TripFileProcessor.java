package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripFileProcessor implements GtfsFileProcessor{

    private final TripJpaRepository tripRepository;
    private final RouteJpaRepository routeRepository;
    private final ServiceCalendarJpaRepository calendarRepository;
    private final BlockFileProcessor blockFileProcessor;
    private final EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Processing trips.txt for city: {} (Source: {})", cityName, source);

        // Pre-load Routes and Calendars to RAM for O(1) lookup
        Map<String, RouteEntity> routeMap = routeRepository.findAllByCity(cityName).stream()
                .collect(Collectors.toMap(RouteEntity::getRouteIdExternal, r -> r));

        Map<String, ServiceCalendarEntity> calendarMap = calendarRepository.findAllByCity(cityName).stream()
                .collect(Collectors.toMap(ServiceCalendarEntity::getServiceIdExternal, c -> c));


        CsvParser parser = createCsvParser();
        List<TripEntity> buffer = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                RouteEntity route = routeMap.get(row[1]);
                ServiceCalendarEntity calendar = calendarMap.get(row[2]);

                if (route == null || calendar == null) {
                    continue; // Skip malformed or missing dependencies
                }

                buffer.add(mapRowToEntity(row, route, calendar, cityName));

                if (buffer.size() >= BATCH_SIZE) {
                    flushAndClear(buffer);
                    totalSaved += buffer.size();
                    buffer.clear();
                }
            } catch (Exception e) {
                log.warn("Error parsing trip row: {}", e.getMessage());
            }
        }

        if (!buffer.isEmpty()) {
            flushAndClear(buffer);
            totalSaved += buffer.size();
        }

        log.info("Successfully imported {} trips for {}", totalSaved, cityName);
    }

    private void flushAndClear(List<TripEntity> batch) {
        tripRepository.saveAll(batch);
        entityManager.flush(); // push changes to the DB
        entityManager.clear();
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
    public void clear(String cityName) {
        log.info("Cleaning up trips for city: {}", cityName);
        tripRepository.deleteTripByCityBulk(cityName);

    }
    private TripEntity mapRowToEntity(String[] row, RouteEntity route, ServiceCalendarEntity calendar, String cityName) {
        String blockId = row[6];
        String shift = blockFileProcessor.getShiftByBlockId(blockId, cityName);

        return TripEntity.builder()
                .tripIdExternal(row[0])
                .route(route)
                .city(cityName)
                .calendar(calendar)
                .headsign(row[3])
                .directionId(parseNullableInt(row[5]))
                .blockId(blockId)
                .shift(shift)
                .shapeId(row[7])
                .wheelchairAccessible(parseNullableIntOrDefault(row[8]))
                .build();
    }

    private Integer parseNullableInt(String value) {
        return (value == null || value.isBlank()) ? null : Integer.valueOf(value);
    }

    private Integer parseNullableIntOrDefault(String value) {
        return (value == null || value.isBlank()) ? 0 : Integer.parseInt(value);
    }

}

