package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import com.univocity.parsers.csv.CsvParser;
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

        List<TripEntity> trips = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            String routeExtId = row[1];
            String serviceExtId = row[2];

            RouteEntity route = routeMap.get(routeExtId);
            ServiceCalendarEntity calendar = calendarMap.get(serviceExtId);

            if (route == null || calendar == null) {
                log.warn("Skipping trip {} due to missing Route ({}) or Calendar ({})", row[0], routeExtId, serviceExtId);
                continue;
            }

                buffer.add(mapRowToEntity(row, route, calendar, cityName));

            if (trips.size() >= BATCH_SIZE) {
                tripRepository.saveAll(trips);
                totalSaved += trips.size();
                trips.clear();
            }
        }

        if (!trips.isEmpty()) {
            tripRepository.saveAll(trips);
            totalSaved += trips.size();
        }

        log.info("Successfully imported {} trips for {}", totalSaved, cityName);
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
    private TripEntity mapRowToEntity(String[] row, RouteEntity route, ServiceCalendarEntity calendar) {
        String blockId = row[6];
        String shift = blockFileProcessor.getShiftByBlockId(blockId, cityName);

        return TripEntity.builder()
                .tripIdExternal(row[0])
                .route(route)
                .city(cityName)
                .calendar(calendar)
                .headsign(row[3])
                .directionId(row[5] != null ? Integer.parseInt(row[5]) : null)
                .blockId(blockId)
                .shift(shift)
                .shapeId(row[7])
                .wheelchairAccessible(row[8] != null ? Integer.parseInt(row[8]) : 0)
                .build();
    }
}

