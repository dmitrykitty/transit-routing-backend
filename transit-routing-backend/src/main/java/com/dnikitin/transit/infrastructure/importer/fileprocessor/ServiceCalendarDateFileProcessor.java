package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarDateJpaRepository;
import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCalendarDateFileProcessor implements GtfsFileProcessor {
    private static final DateTimeFormatter GTFS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ServiceCalendarDateJpaRepository calendarDateRepository;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing calendar_dates.txt for city: {} (Source: {})", city.getName(),  source);

        Set<String> existingExceptions = calendarDateRepository.findAllByCity(city).stream()
                .map(this::createCompositeKey)
                .collect(Collectors.toSet());

        CsvParser parser = createCsvParser();
        List<ServiceCalendarDateEntity> batch = new ArrayList<>();

        for (String[] row : parser.iterate(inputStream)) {
            // creating key for current row
            String serviceId = row[0];
            String dateStr = row[1];
            String typeStr = row[2];
            String currentKey = serviceId + "-" + dateStr + "-" + typeStr;

            if (existingExceptions.contains(currentKey)) {
                continue;
            }

            batch.add(mapToEntity(row, city));
            existingExceptions.add(currentKey);
        }

        if (!batch.isEmpty()) {
            calendarDateRepository.saveAll(batch);
            log.info("Imported {} new service calendar exceptions for {}", batch.size(), city.getName());
        } else {
            log.info("No new service calendar exceptions to import for {}", city.getName());
        }
    }

    private String createCompositeKey(ServiceCalendarDateEntity entity) {
        String dateFormatted = entity.getDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        return entity.getServiceIdExternal() + "-" + dateFormatted + "-" + entity.getExceptionType();
    }

    @Override
    public String getName() {
        return "calendar_dates.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 6;
    }

    @Override
    public void clear(CityEntity city) {
        log.info("Cleaning up calendar dates for city: {}", city.getName());
        calendarDateRepository.deleteServiceCalendarDateByCityBulk(city);
    }

    private ServiceCalendarDateEntity mapToEntity(String[] row, CityEntity city) {
        return ServiceCalendarDateEntity.builder()
                .serviceIdExternal(row[0])
                .date(LocalDate.parse(row[1], GTFS_DATE_FORMATTER))
                .exceptionType(Integer.parseInt(row[2]))
                .city(city)
                .build();
    }
}
