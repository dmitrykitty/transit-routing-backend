package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record RaptorDataSet(
        Map<Integer, StopRaptor> stopsById,
        Map<Integer, RouteRaptor> routesById,
        List<Integer> routeIds
) implements Serializable {
}
