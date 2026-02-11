package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.importer.fileprocessor.GtfsFileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.File;
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
    public void performFullCityUpdate(String cityName, Map<String, String> sourceUrls) {
        log.info("Starting orchestrated atomic update for city: {}", cityName);

        try {
            clearCityData(cityName);

            for (Map.Entry<String, String> entry : sourceUrls.entrySet()) {
                String sourceId = entry.getKey(); //"KRK_A"
                String url = entry.getValue();

                log.info("Downloading and processing bundle: {} from {}", sourceId, url);
                processZipBundle(url, cityName, sourceId);
            }

            log.info("Full update for {} completed successfully.", cityName);
        } catch (Exception e) {
            log.error("Critical error during city update: {}", e.getMessage(), e);
            throw new RuntimeException("City update failed - rolling back transaction", e);
        }
    }

    private void processZipBundle(String url, String cityName, String sourceId) throws IOException {
        Path tempDir = Files.createTempDirectory("gtfs_import_" + sourceId + "_");
        Map<String, Path> extractedFiles = new HashMap<>();

        try (InputStream in = restTemplate.getForObject(url, InputStream.class);
             ZipInputStream zis = new ZipInputStream(Objects.requireNonNull(in))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                if (processorsMap.containsKey(fileName)) {
                    Path tempFile = tempDir.resolve(fileName);
                    Files.copy(zis, tempFile);
                    extractedFiles.put(fileName, tempFile);
                }
                zis.closeEntry();
            }

            for (String fileName : IMPORT_ORDER) {
                Path file = extractedFiles.get(fileName);
                if (file != null) {
                    processFile(fileName, file, cityName, sourceId);
                    extractedFiles.remove(fileName);
                }
            }

            for (Map.Entry<String, Path> remaining : extractedFiles.entrySet()) {
                processFile(remaining.getKey(), remaining.getValue(), cityName, sourceId);
            }

        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    private void processFile(String fileName, Path filePath, String cityName, String sourceId) {
        GtfsFileProcessor processor = processorsMap.get(fileName);
        log.info("Running processor {} for file: {}", processor.getClass().getSimpleName(), fileName);

        try (InputStream is = Files.newInputStream(filePath)) {
            processor.process(is, cityName, sourceId);
        } catch (IOException e) {
            log.error("Failed to read temp file {}: {}", fileName, e.getMessage());
        }
    }

    private void clearCityData(String cityName) {
        log.warn("Clearing database for city: {} using processor priorities", cityName);
        processorList.stream()
                .sorted(Comparator.comparingInt(GtfsFileProcessor::getDeletionPriority).reversed())
                .forEach(processor -> {
                    log.debug("Cleaning {} table...", processor.getName());
                    processor.clear();
                });
    }

    private void cleanupTempDirectory(Path dir) {
        try (var files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.error("Failed to cleanup temp directory {}: {}", dir, e.getMessage());
        }
    }
}
