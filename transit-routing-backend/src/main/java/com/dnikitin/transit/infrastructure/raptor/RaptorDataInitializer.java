package com.dnikitin.transit.infrastructure.raptor;

import com.dnikitin.transit.application.port.out.RaptorQueryPort;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class RaptorDataInitializer {

    private final RaptorQueryPort raptorRepository;
    private final RaptorImportService raptorImportService;
    private final CityJpaRepository cityRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        log.info("Verifying RAPTOR cache integrity for all cities...");

        List<CityEntity> cities = cityRepository.findAll();

        if (cities.isEmpty()) {
            log.info("No cities found in SQL database. Waiting for initial import.");
            return;
        }

        for (CityEntity city : cities) {
            boolean isCached = raptorRepository.isDataAvailable(city.getId());

            if (isCached) {
                log.info("City '{}' (ID: {}) is already cached in Redis.", city.getName(), city.getId());
            } else {
                log.info("City '{}' (ID: {}) missing in Redis. Starting import from SQL...", city.getName(), city.getId());
                try {
                    raptorImportService.importDataToRedis(city);
                } catch (Exception e) {
                    log.error("Failed to warm up RAPTOR cache for city: {}", city.getName(), e);
                }
            }
        }
    }
}
