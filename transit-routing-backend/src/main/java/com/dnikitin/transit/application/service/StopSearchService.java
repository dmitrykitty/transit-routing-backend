package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.out.StopQueryPort;
import com.dnikitin.transit.domain.model.Stop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StopSearchService {
    private final StopQueryPort stopQueryPort;

    public List<Stop> findAll() {
        return stopQueryPort.getStops();
    }
}
