package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.*;
import com.dnikitin.transit.infrastructure.repository.AgencyJpaRepository;
import com.dnikitin.transit.infrastructure.repository.RouteJpaRepository;
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
public class RouteFileProcessor implements GtfsFileProcessor {
    private final RouteJpaRepository routeRepository;
    private final AgencyJpaRepository agencyRepository;
    private final EntityManager entityManager;
    private static final int BATCH_SIZE = 500;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing routes.txt for city: {} from source: {}", city.getName(), source);

        Map<String, AgencyEntity> agencyMap = agencyRepository.findAllByCity(city).stream()
                .collect(Collectors.toMap(AgencyEntity::getAgencyIdExternal, a -> a));

        VehicleType vehicleType = determineVehicleType(source);
        CsvParser parser = createCsvParser();

        List<RouteEntity> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                batch.add(mapToEntity(row, agencyMap, vehicleType, city));

                if (batch.size() >= BATCH_SIZE) {
                    saveAndClear(batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            } catch (Exception e) {
                log.warn("Failed to map route row: {}", (Object) row);
            }
        }
        if (!batch.isEmpty()) {
            saveAndClear(batch);
            totalSaved += batch.size();
        }

        log.info("Imported {} routes for {} from {}", totalSaved, city.getName(), source);
    }

    private void saveAndClear(List<RouteEntity> batch) {
        routeRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
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

    private RouteEntity mapToEntity(
            String[] row,
            Map<String, AgencyEntity> agencyMap,
            VehicleType vehicleType,
            CityEntity city) {
        return RouteEntity.builder()
                .routeIdExternal(row[0])
                .agency(resolveAgency(row[1], agencyMap))
                .routeNumber(row[2])
                .name(row[3])
                .vehicleType(vehicleType)
                .city(city)
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
