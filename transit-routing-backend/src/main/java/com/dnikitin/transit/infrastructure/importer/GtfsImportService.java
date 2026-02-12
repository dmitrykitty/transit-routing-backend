package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.importer.fileprocessor.GtfsFileProcessor;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class GtfsImportService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, GtfsFileProcessor> processorsMap;
    private final List<GtfsFileProcessor> processorList;

    private static final List<String> IMPORT_ORDER = List.of(
            "agency.txt",
            "calendar.txt",
            "calendar_dates.txt",
            "blocks.txt",
            "routes.txt",
            "shapes.txt",
            "stops.txt",
            "trips.txt",
            "stop_times.txt"
    );

    public GtfsImportService(List<GtfsFileProcessor> processors) {
        this.processorList = processors;
        this.processorsMap = processors.stream()
                .collect(Collectors.toMap(GtfsFileProcessor::getName, Function.identity()));
    }

    @Transactional
    public void performFullCityUpdate(CityEntity city, Map<String, String> sourceUrls) {
        log.info("Starting orchestrated atomic update for city: {}", city.getName());
        clearCityData(city);

        try {

            for (Map.Entry<String, String> entry : sourceUrls.entrySet()) {
                String sourceId = entry.getKey(); //"KRK_A"
                String url = entry.getValue();

                log.info("Downloading and processing bundle: {} from {}", sourceId, url);
                processZipBundle(url, city, sourceId);
            }

            log.info("Full update for {} completed successfully.", city.getName());
        } catch (Exception e) {
            log.error("Critical error during city update: {}", e.getMessage(), e);
            throw new RuntimeException("City update failed - rolling back transaction", e);
        }
    }

    private void processZipBundle(String url, CityEntity city, String sourceId) throws IOException {
        Path tempDir = Files.createTempDirectory("gtfs_import_" + sourceId + "_");
        Map<String, Path> extractedFiles = new HashMap<>();

        try {
            restTemplate.execute(url, org.springframework.http.HttpMethod.GET, null, response -> {
                try (ZipInputStream zis = new ZipInputStream(response.getBody())) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String fileName = entry.getName();
                        if (processorsMap.containsKey(fileName)) {
                            Path tempFile = tempDir.resolve(fileName);
                            Files.copy(zis, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            extractedFiles.put(fileName, tempFile);
                        }
                        zis.closeEntry();
                    }
                }
                return null;
            });

            for (String fileName : IMPORT_ORDER) {
                Path file = extractedFiles.get(fileName);
                if (file != null) {
                    processFile(fileName, file, city, sourceId);
                    extractedFiles.remove(fileName);
                }
            }

            for (Map.Entry<String, Path> remaining : extractedFiles.entrySet()) {
                processFile(remaining.getKey(), remaining.getValue(), city, sourceId);
            }

        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    private void processFile(String fileName, Path filePath, CityEntity city, String sourceId) {
        GtfsFileProcessor processor = processorsMap.get(fileName);
        log.info("Running processor {} for file: {}", processor.getClass().getSimpleName(), fileName);

        try (InputStream is = Files.newInputStream(filePath)) {
            processor.process(is, city, sourceId);
        } catch (IOException e) {
            log.error("Failed to read temp file {}: {}", fileName, e.getMessage());
        }
    }

    private void clearCityData(CityEntity city) {
        log.warn("Clearing database for city: {} using processor priorities", city.getName());
        processorList.stream()
                .sorted(Comparator.comparingInt(GtfsFileProcessor::getDeletionPriority).reversed())
                .forEach(processor -> {
                    log.info("Clearing {} for city {}", processor.getName(), city.getName());
                    processor.clear(city);
                });
    }

    private void cleanupTempDirectory(Path dir) {
        try (var files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        boolean deleted = file.delete();
                        if(deleted) {
                            log.info("Deleted temp file {} inside {}", file.getName(), dir);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to cleanup temp directory {}: {}", dir, e.getMessage());
        }
    }
}
