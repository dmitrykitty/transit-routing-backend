package com.dnikitin.transit.infrastructure.importer;

import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GtfsImportService {

    private final StopJpaRepository stopRepository; //
    private final TripJpaRepository tripRepository;

    @Transactional
    public void performFullCityUpdate(String cityName, List<String> urls) {
        log.info("Starting atomic update for city: {}", cityName);

        try {
            clearCityData(cityName);

            for (String url : urls) {
                log.info("Importing partial data from: {}", url);
                // logic to download, unzip and save to DB
                // remember to use batch inserts!
            }

            log.info("Full update for {} completed successfully.", cityName);
        } catch (Exception e) {
            log.error("Critical error during city update: {}", e.getMessage());
            throw new RuntimeException("City update failed", e);
        }
    }

    private void clearCityData(String cityName) {
        log.warn("Clearing all existing data for city: {}", cityName);
        // Order: StopTimes -> Trips -> Routes -> Stops
        tripRepository.deleteAllInBatch();
        stopRepository.deleteAllInBatch();
    }
}
