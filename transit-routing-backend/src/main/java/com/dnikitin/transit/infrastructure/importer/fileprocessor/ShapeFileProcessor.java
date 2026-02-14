package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.repository.ShapePointJpaRepository;
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
public class ShapeFileProcessor implements GtfsFileProcessor {
    private static final int BATCH_SIZE = 5000;

    private final JdbcTemplate jdbcTemplate;
    private final ShapePointJpaRepository shapePointRepository;

    private static final String INSERT_SHAPE_SQL = """
            INSERT INTO shape_point (
                shape_id_ext,
                point_sequence,
                city_id,
                location
            )
            VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))
            ON CONFLICT (shape_id_ext, point_sequence, city_id) DO NOTHING
            """;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing shapes.txt for city: {} (Source: {})", city.getName(), source);

        CsvParser parser = createCsvParser();
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;

        for (String[] row : parser.iterate(inputStream)) {
            try {

                batch.add(new Object[]{
                        row[0],                                     // shape_id_external
                        Integer.parseInt(row[3]),                   // sequence
                        city.getId(),                               // city_id
                        Double.parseDouble(row[2]),                 // x dla ST_MakePoint (longitude)
                        Double.parseDouble(row[1])                  // y dla ST_MakePoint (latitude)
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(INSERT_SHAPE_SQL, batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Progress: {} shape points imported", totalSaved);
                }
            } catch (Exception e) {
                log.warn("Skipping malformed shape row: {}. Reason: {}", row, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_SHAPE_SQL, batch);
            totalSaved += batch.size();
        }

        log.info("Import finished. Total shape points for {}: {}", city.getName(), totalSaved);
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
    public void clear(CityEntity city) {
        log.info("Cleaning up calendar for city: {}", city.getName());
        shapePointRepository.deleteShapePointByCityBulk(city);
    }

}
