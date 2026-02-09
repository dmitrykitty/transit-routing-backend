package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.out.StopSearchPort;
import com.dnikitin.transit.domain.model.Stop;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopSearchService {
    private final StopSearchPort stopSearchPort;

    public StopSearchService(StopSearchPort stopSearchPort) {
        this.stopSearchPort = stopSearchPort;
    }

    public List<Stop> findAll() {
        return stopSearchPort.getStops();
    }
}
