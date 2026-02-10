package com.dnikitin.transit.infrastructure.scheduler;

import com.dnikitin.transit.infrastructure.importer.GtfsImportService;
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
    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, String> krkUrls = Map.of(
            "KRK_A", "https://gtfs.ztp.krakow.pl/GTFS_KRK_A.zip",
            "KRK_T", "https://gtfs.ztp.krakow.pl/GTFS_KRK_T.zip",
            "KRK_M", "https://gtfs.ztp.krakow.pl/GTFS_KRK_M.zip"
    );

    @Scheduled(cron = "0 0 3 * * MON") // Every Monday at 03:00 AM
    public void checkAllKrakowFiles() {
        log.info("Starting weekly check for all Krakow GTFS files.");
        boolean anyUpdateNeeded = false;

        for (Map.Entry<String, String> entry : krkUrls.entrySet()) {
            String fileId = entry.getKey();
            String url = entry.getValue();

            var metadata = metadataRepository.findById(fileId)
                    .orElse(new DataImportMetadataEntity(
                            fileId,
                            "Kraków",
                            null,
                            ""));

            HttpHeaders headers = restTemplate.headForHeaders(url);
            String remoteHeader = headers.getFirst("Last-Modified");

            if (remoteHeader != null && !remoteHeader.equals(metadata.getLastModifiedHeader())) {
                log.info("Update detected for file: {}", fileId);
                anyUpdateNeeded = true;
                metadata.setLastModifiedHeader(remoteHeader);
                metadata.setLastImportTimestamp(LocalDateTime.now());
                metadataRepository.save(metadata);
            }
        }

        if (anyUpdateNeeded) {
            log.info("One or more files changed. Triggering full re-import for Krakow.");
            importService.performFullCityUpdate("Kraków", krkUrls.values().stream().toList());
        } else {
            log.info("All Krakow transit files are up-to-date.");
        }
    }
}