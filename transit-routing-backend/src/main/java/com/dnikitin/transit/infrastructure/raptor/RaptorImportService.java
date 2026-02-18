package com.dnikitin.transit.infrastructure.raptor;

import com.dnikitin.transit.application.port.out.RaptorQueryPort;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaptorImportService {

    private final TripJpaRepository tripRepository;
    private final StopJpaRepository stopRepository;

    private final RaptorQueryPort raptorQueryPort;

    @Transactional(readOnly = true)
    public void importDataToRedis(CityEntity city) {
        log.info("Starting RAPTOR data import...");

        List<StopEntity> allStops = stopRepository.findAll();

    }
}
