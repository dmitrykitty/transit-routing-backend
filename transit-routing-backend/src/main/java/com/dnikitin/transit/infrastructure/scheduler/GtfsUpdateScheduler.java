package com.dnikitin.transit.infrastructure.scheduler;

import com.dnikitin.transit.infrastructure.importer.GtfsImportService;
import com.dnikitin.transit.infrastructure.importer.GtfsProperties;
import com.dnikitin.transit.infrastructure.persistence.entity.DataImportMetadataEntity;
import com.dnikitin.transit.infrastructure.repository.DataImportMetadataJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsUpdateScheduler {

    private final GtfsImportService importService;
    private final DataImportMetadataJpaRepository metadataRepository;
    private final GtfsProperties gtfsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "${gtfs.update-cron:0 0 3 * * MON}")
    public void checkAndSyncAllCities() {
        log.info("Starting global GTFS synchronization check for all configured cities.");

        gtfsProperties.getCities().forEach((cityName, sources) -> {
            try {
                processCityUpdate(cityName, sources);
            } catch (Exception e) {
                log.error("Failed to sync city {}: {}", cityName, e.getMessage());
            }
        });
    }

    private void processCityUpdate(String cityName, Map<String, String> sources) {
        log.info("Checking update status for city: {}", cityName);
        boolean cityRequiresUpdate = false;

        for (Map.Entry<String, String> entry : sources.entrySet()) {
            String sourceId = entry.getKey();
            String url = entry.getValue();

            if (isUpdateRequired(sourceId, url, cityName)) {
                log.info("Change detected in source: {} for city: {}", sourceId, cityName);
                cityRequiresUpdate = true;
            }
        }

        if (cityRequiresUpdate) {
            log.info("Triggering full re-import for city: {} due to source changes.", cityName);
            importService.performFullCityUpdate(cityName, sources);

            updateMetadataTimestamps(sources);
        } else {
            log.info("All sources for {} are up-to-date.", cityName);
        }
    }

    private boolean isUpdateRequired(String sourceId, String url, String cityName) {
        try {
            HttpHeaders headers = restTemplate.headForHeaders(url);
            String remoteLastModified = headers.getFirst("Last-Modified");

            if (remoteLastModified == null) {
                log.warn("Source {} did not return Last-Modified header. Forcing check.", sourceId);
                return true;
            }

            var metadata = metadataRepository.findById(sourceId)
                    .orElseGet(() -> new DataImportMetadataEntity(sourceId, cityName, null, ""));

            boolean hasChanged = !remoteLastModified.equals(metadata.getLastModifiedHeader());

            if (hasChanged) {
                metadata.setLastModifiedHeader(remoteLastModified);
                metadataRepository.save(metadata);
            }

            return hasChanged;
        } catch (Exception e) {
            log.error("Could not check headers for {}: {}", sourceId, e.getMessage());
            return false;
        }
    }

    private void updateMetadataTimestamps(Map<String, String> sources) {
        for (String sourceId : sources.keySet()) {
            metadataRepository.findById(sourceId).ifPresent(m -> {
                m.setLastImportTimestamp(LocalDateTime.now());
                metadataRepository.save(m);
            });
        }
    }
}