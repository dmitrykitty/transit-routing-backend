package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.importer.util.ParsingUtil;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
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
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCalendarFileProcessor implements GtfsFileProcessor {
    private static final DateTimeFormatter GTFS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ServiceCalendarJpaRepository calendarRepository;

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Processing calendar.txt for city: {} (Source: {})", city.getName(), source);

        Set<String> existingServiceIds = calendarRepository.findAllByCity(city).stream()
                .map(ServiceCalendarEntity::getServiceIdExternal)
                .collect(Collectors.toSet());

        CsvParser parser = createCsvParser();
        List<ServiceCalendarEntity> batch = new ArrayList<>();

        for (String[] row : parser.iterate(inputStream)) {
            String serviceExtId = row[0];


            if (existingServiceIds.contains(serviceExtId)) {
                continue;
            }

            batch.add(mapToEntity(row, city));
            existingServiceIds.add(serviceExtId);
        }

        if (!batch.isEmpty()) {
            calendarRepository.saveAll(batch);
            log.info("Imported {} new service calendars for {}", batch.size(), city.getName());
        } else {
            log.info("No new service calendars to import for {}", city.getName());
        }
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
    public void clear(CityEntity city) {
        log.info("Cleaning up calendar for city: {}", city.getName());
        calendarRepository.deleteServiceCalendarByCityBulk(city);
    }

    private ServiceCalendarEntity mapToEntity(String[] row, CityEntity city) {
        return ServiceCalendarEntity.builder()
                .serviceIdExternal(row[0])
                .monday(ParsingUtil.parseBoolean(row[1]))
                .tuesday(ParsingUtil.parseBoolean(row[2]))
                .wednesday(ParsingUtil.parseBoolean(row[3]))
                .thursday(ParsingUtil.parseBoolean(row[4]))
                .friday(ParsingUtil.parseBoolean(row[5]))
                .saturday(ParsingUtil.parseBoolean(row[6]))
                .sunday(ParsingUtil.parseBoolean(row[7]))
                .startDate(LocalDate.parse(row[8], GTFS_DATE_FORMATTER))
                .endDate(LocalDate.parse(row[9], GTFS_DATE_FORMATTER))
                .city(city)
                .build();
    }
}
