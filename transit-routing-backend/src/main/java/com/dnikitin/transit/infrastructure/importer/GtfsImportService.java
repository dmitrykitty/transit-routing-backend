package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.importer.fileprocessor.GtfsFileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public GtfsImportService(List<GtfsFileProcessor> processors) {
        this.processorList = processors;
        this.processorsMap = processors.stream()
                .collect(Collectors.toMap(GtfsFileProcessor::getName, Function.identity()));
    }

    @Transactional
    public void performFullCityUpdate(String cityName, List<String> urls) {
        log.info("Starting atomic update for city: {}", cityName);

        try {
            clearCityData(cityName);

            for (String url : urls) {
                log.info("Importing partial data from: {}", url);
                processZipBundle(url, cityName);
            }

            log.info("Full update for {} completed successfully.", cityName);
        } catch (Exception e) {
            log.error("Critical error during city update: {}", e.getMessage());
            throw new RuntimeException("City update failed", e);
        }
    }

    private void processZipBundle(String url, String cityName){
        InputStream in = restTemplate.getForObject(url, InputStream.class);
        if (in == null) return;

        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                GtfsFileProcessor gtfsFileProcessor = processorsMap.get(entry.getName());
                if (gtfsFileProcessor != null) {
                    gtfsFileProcessor.process(zis, cityName);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Error processing zip entry: {}", e.getMessage());
        }
    }

    private void clearCityData(String cityName) {
        log.warn("Clearing all existing data for city: {}", cityName);
        // Order: StopTimes -> Trips -> Routes -> Stops
        processorList.stream()
                .sorted(Comparator.comparingInt(GtfsFileProcessor::getDeletionPriority).reversed())
                .forEach(processor -> {
                    log.info("Cleaning data via processor: {}", processor.getClass().getSimpleName());
                    processor.clear();
                });
    }
}
