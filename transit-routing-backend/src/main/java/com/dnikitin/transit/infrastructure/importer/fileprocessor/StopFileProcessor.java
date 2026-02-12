package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.univocity.parsers.csv.CsvParser;
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

    private static final int INTERNAL_BATCH_SIZE = 1000;

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Processing stops.txt for city: {} (Source: {})", cityName, source);

        CsvParser parser = createCsvParser();

        List<StopEntity> stops = new ArrayList<>(INTERNAL_BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                StopEntity stop = mapRowToEntity(row, cityName);

                stops.add(stop);

                if (stops.size() >= INTERNAL_BATCH_SIZE) {
                    stopRepository.saveAll(stops);
                    totalSaved += stops.size();
                    log.debug("Intermediate save: {} stops persisted so far", totalSaved);
                    stops.clear();
                }
            } catch (Exception e) {
                log.error("Error at row {}: {}", totalSaved + stops.size(), e.getMessage());
            }
        }

        if(!stops.isEmpty()) {
            stopRepository.saveAll(stops);
            totalSaved += stops.size();
        }
        log.info("Import finished. Total stops persisted for {}: {}", cityName, totalSaved);
    }

    private StopEntity mapRowToEntity(String[] row) {
        double lat = Double.parseDouble(row[4]); // stop_lat
        double lon = Double.parseDouble(row[5]); // stop_lon

        return StopEntity.builder()
                .stopIdExternal(row[0])
                .stopCode(row[1])
                .name(row[2])
                .description(row[3])
                .location(geometryFactory.createPoint(new Coordinate(lat, lon)))
                .build();
    }

    @Override
    public String getName() {
        return "stop.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 1;
    }

    @Override
    public void clear(String cityName) {
        log.info("Cleaning up stop for city: {}", cityName);
        stopRepository.deleteStopByCityBulk(cityName);
    }
}
