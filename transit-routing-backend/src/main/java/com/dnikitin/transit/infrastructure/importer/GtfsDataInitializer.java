package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsDataInitializer {

    private final CityJpaRepository cityRepository;
    private final GtfsImportService gtfsImportService;
    private final GtfsProperties gtfsProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Checking if database needs initial seeding for multiple cities...");

        if (cityRepository.count() == 0) {
            log.info("Database is empty. Starting multi-city GTFS import.");

            gtfsProperties.getCities().forEach((cityName, urls) -> {
                log.info("Preparing city entity for: {}", cityName);
                CityEntity city = getOrCreateCity(cityName);
                gtfsImportService.performFullCityUpdate(city, urls);

            });

            log.info("Initial multi-city seed completed.");
        } else {
            log.info("Database already contains city data. Skipping initial seed.");
        }
    }

    private CityEntity getOrCreateCity(String cityName) {
        return cityRepository.findByName(cityName)
                .orElseGet(() -> cityRepository.save(CityEntity.builder()
                        .name(cityName)
                        .build()));
    }
}
