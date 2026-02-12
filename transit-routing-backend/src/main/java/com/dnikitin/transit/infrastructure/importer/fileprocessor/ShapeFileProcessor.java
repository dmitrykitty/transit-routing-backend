package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.ShapePointEntity;
import com.dnikitin.transit.infrastructure.repository.ShapePointJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShapeFileProcessor implements GtfsFileProcessor {
    private static final int BATCH_SIZE = 5000;

    private final ShapePointJpaRepository shapePointRepository;
    private final EntityManager entityManager;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Processing shapes.txt for city: {} (Source: {})", cityName, source);

        CsvParser parser = createCsvParser();
        List<ShapePointEntity> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                double lat = Double.parseDouble(row[1]);
                double lon = Double.parseDouble(row[2]);

                batch.add(mapToEntity(row, lat, lon, cityName));

                if (batch.size() >= BATCH_SIZE) {
                    saveAndFlush(batch);
                    totalSaved += batch.size();
                    batch.clear();

                    log.info("Partition import: imported {} shape_files", totalSaved);
                }
            } catch (Exception e) {
                log.warn("Skipping malformed shape point row: {}", (Object) row);
            }
        }

        if (!batch.isEmpty()) {
            saveAndFlush(batch);
            totalSaved += batch.size();
        }

        log.info("Imported {} shape points for {} from {}", totalSaved, cityName, source);
    }

    @Override
    public String getName() {
        return "shapes.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 4;
    }

    @Override
    public void clear(String cityName) {
        log.info("Cleaning up calendar for city: {}", cityName);
        shapePointRepository.deleteShapePointByCityBulk(cityName);
    }

    private ShapePointEntity mapToEntity(String[] row, double lat, double lon, String cityName) {
        return ShapePointEntity.builder()
                .shapeIdExternal(row[0])
                .location(geometryFactory.createPoint(new Coordinate(lon, lat)))
                .sequence(Integer.parseInt(row[3]))
                .distTraveled(parseNullableDouble(row[4]))
                .city(cityName)
                .build();
    }

    private void saveAndFlush(List<ShapePointEntity> batch) {
        shapePointRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
    }

    private Double parseNullableDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
