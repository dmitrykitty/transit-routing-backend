package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsDataInitializer {

    private final StopJpaRepository stopJpaRepository;
    private final GtfsImportService gtfsImportService;
    private static final String KRK_GTFS_URL = "https://gtfs.ztp.krakow.pl/";

    @EventListener(ApplicationEvent.class)
    public void onApplicationReady(){
        log.info("Checking if database needs initial seeding...");
        if (stopJpaRepository.count() == 0) {
            log.info("Database is empty. Triggering initial GTFS import for Krakow.");
            gtfsImportService.performUpdate(KRK_GTFS_URL);
        } else {
            log.info("Database already contains transit data. Skipping initial seed.");
        }
    }
}
