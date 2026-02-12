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
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgencyFileProcessor implements GtfsFileProcessor {

    private final AgencyJpaRepository agencyRepository;

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Processing agency.txt for city: {} (Source: {})", cityName, source);

        Set<String> existingAgencyIds = agencyRepository.findAllByCity(cityName).stream()
                .map(AgencyEntity::getAgencyIdExternal)
                .collect(Collectors.toSet());

        CsvParser parser = createCsvParser();
        List<AgencyEntity> batch = new ArrayList<>();

        for (String[] row : parser.iterate(inputStream)) {
            String agencyExtId = row[0];

            if (existingAgencyIds.contains(agencyExtId)) {
                log.debug("Agency {} already exists for city {}, skipping", agencyExtId, cityName);
                continue;
            }

            batch.add(mapToEntity(row, cityName));
            existingAgencyIds.add(agencyExtId);
        }

        if (!batch.isEmpty()) {
            agencyRepository.saveAll(batch);
            log.info("Imported {} agencies for {}", batch.size(), cityName);
        } else {
            log.info("No new agencies to import for {}", cityName);
        }
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
    public void clear(String cityName) {
        log.info("Cleaning up agencies for city: {}", cityName);
        agencyRepository.deleteAgencyByCityBulk(cityName);
    }

    private AgencyEntity mapToEntity(String[] row, String cityName) {
        return AgencyEntity.builder()
                .agencyIdExternal(row[0])
                .name(row[1])
                .url(row[2])
                .city(cityName)
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
