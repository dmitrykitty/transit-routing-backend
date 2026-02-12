package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
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
public class StopFileProcessor implements GtfsFileProcessor {

    private final StopJpaRepository stopRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing stops.txt for city: {} (Source: {})", city.getName(), source);

        CsvParser parser = createCsvParser();

        List<StopEntity> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                StopEntity stop = mapRowToEntity(row, city);

                batch.add(stop);

                if (batch.size() >= BATCH_SIZE) {
                    saveAndClear(batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            } catch (Exception e) {
                log.error("Error at row {}: {}", totalSaved + batch.size(), e.getMessage());
            }
        }

        if(!batch.isEmpty()) {
            saveAndClear(batch);
            totalSaved += batch.size();
        }
        log.info("Import finished. Total stops persisted for {}: {}", city.getName(), totalSaved);
    }

    private StopEntity mapRowToEntity(String[] row, CityEntity city) {
        double lat = Double.parseDouble(row[4]); // stop_lat
        double lon = Double.parseDouble(row[5]); // stop_lon

        return StopEntity.builder()
                .stopIdExternal(row[0])
                .stopCode(row[1])
                .name(row[2])
                .description(row[3])
                .city(city)
                .location(geometryFactory.createPoint(new Coordinate(lon, lat)))
                .build();
    }

    private void saveAndClear(List<StopEntity> batch) {
        stopRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public String getName() {
        return "stops.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 1;
    }

    @Override
    public void clear(CityEntity city) {
        log.info("Cleaning up stop for city: {}", city.getName());
        stopRepository.deleteStopByCityBulk(city);
    }
}
