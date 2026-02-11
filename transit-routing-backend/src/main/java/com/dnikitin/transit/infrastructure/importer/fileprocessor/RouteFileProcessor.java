package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.AgencyEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.VehicleType;
import com.dnikitin.transit.infrastructure.repository.AgencyJpaRepository;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
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
public class RouteFileProcessor implements GtfsFileProcessor {
    private final RouteJpaRepository routeRepository;
    private final AgencyJpaRepository agencyRepository;
    private static final int BATCH_SIZE = 500;

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Processing routes.txt for city: {} from source: {}", cityName, source);

        Map<String, AgencyEntity> agencyMap = agencyRepository.findAll().stream()
                .collect(Collectors.toMap(AgencyEntity::getAgencyIdExternal, a -> a));

        VehicleType vehicleType = determineVehicleType(source);
        CsvParser parser = createCsvParser();

        List<RouteEntity> batch = new ArrayList<>(BATCH_SIZE);

        for (String[] row : parser.iterate(inputStream)) {
            try {
                batch.add(mapToEntity(row, agencyMap, vehicleType, cityName));
            } catch (Exception e) {
                log.warn("Failed to map route row: {}", (Object) row);
            }

            if (batch.size() >= BATCH_SIZE) {
                routeRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) routeRepository.saveAll(batch);
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
    public void clear() {
        routeRepository.deleteAllInBatch();
    }
    private RouteEntity mapToEntity(
            String[] row,
            Map<String, AgencyEntity> agencyMap,
            VehicleType vehicleType,
            String cityName) {
        return RouteEntity.builder()
                .routeIdExternal(row[0])
                .agency(resolveAgency(row[1], agencyMap))
                .routeNumber(row[2])
                .name(row[3])
                .vehicleType(vehicleType)
                .city(cityName)
                .build();
    }

    private VehicleType determineVehicleType(String source) {
        if (source.contains("_T")) return VehicleType.TRAM;
        if (source.contains("_A") || source.contains("_M")) return VehicleType.BUS;
        return VehicleType.BUS; // fallback
    }

    private AgencyEntity resolveAgency(String csvAgencyId, Map<String, AgencyEntity> map) {
        String id = blankToNull(csvAgencyId);
        return (id != null) ? map.get(id) : null;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) ? null : value;
    }
}
