package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.AgencyEntity;
import com.dnikitin.transit.infrastructure.repository.AgencyJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgencyFileProcessor implements GtfsFileProcessor {

    private final AgencyJpaRepository agencyRepository;

    @Override
    public void process(InputStream inputStream, String cityName) {
        log.info("Processing agency.txt for city: {}", cityName);
        CsvParser parser = createCsvParser();

        List<AgencyEntity> batch = new ArrayList<>();
        parser.iterate(inputStream).forEach(row ->
                batch.add(mapToEntity(row))
        );
        agencyRepository.saveAll(batch);
        log.info("Imported {} agencies for {}", batch.size(), cityName);
    }

    @Override
    public String getName() {
        return "agency.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 2;
    }

    @Override
    public void clear() {
        agencyRepository.deleteAllInBatch();
    }

    private AgencyEntity mapToEntity(String[] row) {
        return AgencyEntity.builder()
                .agencyIdExternal(row[0])
                .name(row[1])
                .url(row[2])
                .timezone(row[3])
                .lang(blankToNull(row[4]))
                .phone(blankToNull(row[5]))
                .fareUrl(blankToNull(row[6]))
                .email(blankToNull(row[7]))
                .build();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
