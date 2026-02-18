package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.StopRaptor;

import java.util.Map;
import java.util.Optional;

public interface RaptorRepositoryPort {

    Optional<RouteRaptor> getRoute(Short cityId, String routeId);
    Optional<StopRaptor> getStop(Short cityId, Integer stopId);

    void saveStopMapping(Short cityId, Map<Integer, Long> mapping);
    Map<Integer, Long> getStopMapping(Short cityId);

    void saveRoute(Short cityId, RouteRaptor routeRaptor);
    void saveStop(Short cityId, StopRaptor stopRaptor);
    void markDataAvailable(Short cityId);

    void clearCityData(Short cityId);

    boolean isDataAvailable(Short cityId);
}
