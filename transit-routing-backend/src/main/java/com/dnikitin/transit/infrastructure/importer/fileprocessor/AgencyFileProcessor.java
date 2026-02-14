package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.AgencyEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
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
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing agency.txt for city: {} (Source: {})", city.getName(), source);

        Set<String> existingAgencyIds = agencyRepository.findAllByCity(city).stream()
                .map(AgencyEntity::getAgencyIdExternal)
                .collect(Collectors.toSet());

        CsvParser parser = createCsvParser();
        List<AgencyEntity> batch = new ArrayList<>();

        for (String[] row : parser.iterate(inputStream)) {
            String agencyExtId = row[0];

            if (existingAgencyIds.contains(agencyExtId)) {
                log.debug("Agency {} already exists for city {}, skipping", agencyExtId, city.getName());
                continue;
            }

            batch.add(mapToEntity(row, city));
            existingAgencyIds.add(agencyExtId);
        }

        if (!batch.isEmpty()) {
            agencyRepository.saveAll(batch);
            log.info("Imported {} agencies for {}", batch.size(), city.getName());
        } else {
            log.info("No new agencies to import for {}", city.getName());
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
    public void clear(CityEntity city) {
        log.info("Cleaning up agencies for city: {}", city.getName());
        agencyRepository.deleteAgencyByCityBulk(city);
    }

    private AgencyEntity mapToEntity(String[] row, CityEntity city) {
        return AgencyEntity.builder()
                .agencyIdExternal(row[0])
                .name(row[1])
                .url(row[2])
                .city(city)
                .timezone(row[3])
                .lang(blankToNull(row[4]))
                .phone(blankToNull(row[5]))
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
