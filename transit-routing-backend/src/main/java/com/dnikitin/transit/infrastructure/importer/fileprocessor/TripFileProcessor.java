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
    public void process(InputStream inputStream, String cityName) {
        log.info("Processing trips.txt for city: {}", cityName);

        // Pre-load Routes and Calendars to RAM for O(1) lookup
        Map<String, RouteEntity> routeMap = routeRepository.findAll().stream()
                .filter(r -> r.getCity().equals(cityName))
                .collect(Collectors.toMap(RouteEntity::getRouteIdExternal, r -> r));

        Map<String, ServiceCalendarEntity> calendarMap = calendarRepository.findAll().stream()
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

            TripEntity trip = mapRowToEntity(row, route, calendar);
            trips.add(trip);

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
        return 5;
    }

    @Override
    public void clear() {
        tripRepository.deleteAllInBatch();

    }
    private TripEntity mapRowToEntity(String[] row, RouteEntity route, ServiceCalendarEntity calendar) {
        String blockId = row[6];
        String shift = blockFileProcessor.getBlockShiftMap().get(blockId);

        return TripEntity.builder()
                .tripIdExternal(row[0])
                .route(route)
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

