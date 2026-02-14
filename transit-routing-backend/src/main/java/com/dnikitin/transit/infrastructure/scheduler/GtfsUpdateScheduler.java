package com.dnikitin.transit.infrastructure.scheduler;

import com.dnikitin.transit.infrastructure.importer.GtfsImportService;
import com.dnikitin.transit.infrastructure.importer.GtfsProperties;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.DataImportMetadataEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.DataImportMetadataJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsUpdateScheduler {

    private final GtfsImportService importService;
    private final DataImportMetadataJpaRepository metadataRepository;
    private final CityJpaRepository cityRepository;
    private final GtfsProperties gtfsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "${gtfs.update-cron:0 0 3 * * MON}")
    public void checkAndSyncAllCities() {
        log.info("Starting global GTFS synchronization check for all configured cities.");

        gtfsProperties.getCities().forEach((cityName, sources) -> {
            try {
                CityEntity city = getOrCreateCity(cityName);
                processCityUpdate(city, sources);
            } catch (Exception e) {
                log.error("Failed to sync city {}: {}", cityName, e.getMessage());
            }
        });
    }

    private void processCityUpdate(CityEntity city, Map<String, String> sources) {
        log.info("Checking update status for city: {}", city.getName());

        Map<String, String> newHeaders = new HashMap<>();
        boolean cityRequiresUpdate = false;

        for (Map.Entry<String, String> entry : sources.entrySet()) {
            String sourceId = entry.getKey();
            String url = entry.getValue();

            String newHeader = getNewHeaderIfChanged(sourceId, url);

            if (newHeader != null) {
                log.info("Change detected in source: {} for city: {}", sourceId, city.getName());
                cityRequiresUpdate = true;
                newHeaders.put(sourceId, newHeader);
            }
        }

        if (cityRequiresUpdate) {
            log.info("Triggering full re-import for city: {} due to source changes.", city.getName());
            importService.performFullCityUpdate(city, sources, newHeaders);
        } else {
            log.info("All sources for {} are up-to-date.", city.getName());
        }
    }

    private String getNewHeaderIfChanged(String sourceId, String url) {
        try {
            HttpHeaders headers = restTemplate.headForHeaders(url);
            String remoteHeader = headers.getFirst("Last-Modified");

            if (remoteHeader == null) return "FORCE_UPDATE"; // brak nagłówka = zawsze importuj

            String localHeader = metadataRepository.findById(sourceId)
                    .map(DataImportMetadataEntity::getLastModifiedHeader)
                    .orElse("");

            return remoteHeader.equals(localHeader) ? null : remoteHeader;
        } catch (Exception e) {
            log.error("Check failed for {}: {}", sourceId, e.getMessage());
            return null;
        }
    }

    private CityEntity getOrCreateCity(String cityName) {
        return cityRepository.findByName(cityName)
                .orElseGet(() -> cityRepository.save(CityEntity.builder()
                        .name(cityName)
                        .build()));
    }
}