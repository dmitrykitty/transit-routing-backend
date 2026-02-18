package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.scheduler.GtfsUpdateScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
@Order(value = 1)
public class GtfsDataInitializer {

    private final CityJpaRepository cityRepository;
    private final GtfsUpdateScheduler gtfsUpdateScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Checking if database needs initial seeding for multiple cities...");

        if (cityRepository.count() == 0) {
            log.info("Database is empty. Triggering global synchronization.");
            gtfsUpdateScheduler.checkAndSyncAllCities();
            log.info("Initial multi-city seed completed.");
        } else {
            log.info("Database already contains city data. Skipping initial seed.");
        }
    }
}
