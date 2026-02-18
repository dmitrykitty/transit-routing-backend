package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.raptor.Route;
import com.dnikitin.transit.domain.model.raptor.Stop;

import java.util.Map;
import java.util.Optional;

public interface RaptorQueryPort {

    Optional<Route> getRoute(Short cityId, String routeId);
    Optional<Stop> getStop(Short cityId, Integer stopId);

    void saveStopMapping(Short cityId, Map<Integer, Long> mapping);
    Map<Integer, Long> getStopMapping(Short cityId);

    void saveRoute(Short cityId, Route route);
    void saveStop(Short cityId, Stop stop);

    void clearCityData(Short cityId);

    boolean isDataAvailable(Short cityId);
}
