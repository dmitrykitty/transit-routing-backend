package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.out.StopQueryPort;
import com.dnikitin.transit.domain.model.Stop;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopSearchService {
    private final StopQueryPort stopQueryPort;

    public StopSearchService(StopQueryPort stopQueryPort) {
        this.stopQueryPort = stopQueryPort;
    }

    public List<Stop> findAll() {
        return stopQueryPort.getStops();
    }
}
