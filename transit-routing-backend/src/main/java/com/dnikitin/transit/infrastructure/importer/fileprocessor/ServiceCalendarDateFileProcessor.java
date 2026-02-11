package com.dnikitin.transit.infrastructure.importer.fileprocessor;

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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCalendarDateFileProcessor implements GtfsFileProcessor {
    private static final DateTimeFormatter GTFS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ServiceCalendarDateJpaRepository calendarDateRepository;

    @Override
    public void process(InputStream inputStream, String cityName) {
        log.info("Processing calendar_dates.txt for city: {}", cityName);

        CsvParser parser = createCsvParser();
        List<ServiceCalendarDateEntity> batch = new ArrayList<>();
        parser.iterate(inputStream).forEach(row ->
                    batch.add(mapToEntity(row))
        );

        calendarDateRepository.saveAll(batch);
        log.info("Imported {} service calendar exceptions for {}", batch.size(), cityName);
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
    public void clear() {
        calendarDateRepository.deleteAllInBatch();
    }

    private ServiceCalendarDateEntity mapToEntity(String[] row) {
        return ServiceCalendarDateEntity.builder()
                .serviceIdExternal(row[0])
                .date(LocalDate.parse(row[1], GTFS_DATE_FORMATTER))
                .exceptionType(Integer.parseInt(row[2]))
                .build();
    }
}
