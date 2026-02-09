package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.Stop;

import java.util.List;

public interface StopSearchPort {
    List<Stop> getStops();
}
