package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StopFileProcessor implements GtfsFileProcessor {

    private final StopJpaRepository stopRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 5000;

    private static final String INSERT_STOP_SQL = """
                INSERT INTO stop (stop_id_ext, stop_code, name, description, city_id, location)
                VALUES (?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))
                ON CONFLICT (stop_id_ext, city_id) DO NOTHING
            """;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing stops.txt for city: {} (ID: {}) using JDBC Batch", city.getName(), city.getId());

        CsvParser parser = createCsvParser();

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {
                // row[0]=stop_id, row[1]=stop_code, row[2]=stop_name, row[3]=stop_desc, row[4]=lat, row[5]=lon
                batch.add(new Object[]{
                        row[0],                          // stop_id_external
                        row[1],                          // stop_code
                        row[2],                          // name
                        row[3],                          // description
                        city.getId(),                    // city_id
                        Double.parseDouble(row[5]),      // lon (for ST_MakePoint)
                        Double.parseDouble(row[4])       // lat (for ST_MakePoint)
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(INSERT_STOP_SQL, batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            } catch (Exception e) {
                log.error("Skip row due to error: {}", e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_STOP_SQL, batch);
            totalSaved += batch.size();
        }
        log.info("Finished. Total stops in DB for {}: {}", city.getName(), totalSaved);
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
