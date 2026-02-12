package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarJpaRepository;
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
public class ServiceCalendarFileProcessor implements GtfsFileProcessor {
    private static final DateTimeFormatter GTFS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ServiceCalendarJpaRepository calendarRepository;

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Processing calendar.txt for city: {} (Source: {})", cityName, source);

        CsvParser parser = createCsvParser();
        List<ServiceCalendarEntity> batch = new ArrayList<>();

        parser.iterate(inputStream).forEach(row ->
                batch.add(mapToEntity(row, cityName))
        );

        calendarRepository.saveAll(batch);

        log.info("Imported {} service calendars for {}", batch.size(), cityName);
    }

    @Override
    public String getName() {
        return "calendar.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 6;
    }

    @Override
    public void clear(String cityName) {
        log.info("Cleaning up calendar for city: {}", cityName);
        calendarRepository.deleteServiceCalendarByCityBulk(cityName);
    }

    private ServiceCalendarEntity mapToEntity(String[] row, String cityName) {
        return ServiceCalendarEntity.builder()
                .serviceIdExternal(row[0])
                .monday(parseBooleanFlag(row[1]))
                .tuesday(parseBooleanFlag(row[2]))
                .wednesday(parseBooleanFlag(row[3]))
                .thursday(parseBooleanFlag(row[4]))
                .friday(parseBooleanFlag(row[5]))
                .saturday(parseBooleanFlag(row[6]))
                .sunday(parseBooleanFlag(row[7]))
                .startDate(LocalDate.parse(row[8], GTFS_DATE_FORMATTER))
                .endDate(LocalDate.parse(row[9], GTFS_DATE_FORMATTER))
                .city(cityName)
                .build();
    }

    private boolean parseBooleanFlag(String value) {
        return "1".equals(value);
    }
}
