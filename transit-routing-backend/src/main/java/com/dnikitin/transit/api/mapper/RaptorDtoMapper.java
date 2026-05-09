package com.dnikitin.transit.api.mapper;

import com.dnikitin.transit.api.dto.response.*;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.model.raptor.RaptorJourneyLeg;
import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.StopRaptor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RaptorDtoMapper {

    public RaptorJourneyResponse toJourneyResponse(RaptorJourney journey) {
        return new RaptorJourneyResponse(
                journey.sourceStopId(),
                journey.targetStopId(),
                journey.requestedDepartureTime(),
                journey.arrivalTime(),
                journey.tripCount(),
                journey.transferCount(),
                journey.legs().stream()
                        .map(this::toJourneyLegResponse)
                        .toList()
        );
    }

    public RaptorDataSetSummaryResponse toDataSetSummaryResponse(
            Short cityId,
            RaptorDataSet dataSet,
            int routeLimit,
            int stopLimit
    ) {
        int transferCount = dataSet.stopsById().values().stream()
                .mapToInt(stop -> stop.transfers().size())
                .sum();

        List<RaptorRouteSummaryResponse> routes = dataSet.routesById().values().stream()
                .sorted(Comparator.comparingInt(RouteRaptor::id))
                .limit(routeLimit)
                .map(this::toRouteSummaryResponse)
                .toList();

        List<RaptorStopSummaryResponse> stops = dataSet.stopsById().values().stream()
                .sorted(Comparator.comparingInt(StopRaptor::id))
                .limit(stopLimit)
                .map(this::toStopSummaryResponse)
                .toList();

        return new RaptorDataSetSummaryResponse(
                cityId,
                dataSet.stopsById().size(),
                dataSet.routesById().size(),
                transferCount,
                routes,
                stops
        );
    }

    private RaptorJourneyLegResponse toJourneyLegResponse(RaptorJourneyLeg leg) {
        return new RaptorJourneyLegResponse(
                leg.type().name(),
                leg.fromStopId(),
                leg.fromStopName(),
                leg.toStopId(),
                leg.toStopName(),
                leg.departureTime(),
                leg.arrivalTime(),
                leg.sourceRouteId(),
                leg.tripId(),
                leg.headsign()
        );
    }

    private RaptorRouteSummaryResponse toRouteSummaryResponse(RouteRaptor route) {
        Integer firstStopId = route.stopIds().length == 0 ? null : route.stopIds()[0];
        Integer lastStopId = route.stopIds().length == 0 ? null : route.stopIds()[route.stopIds().length - 1];

        return new RaptorRouteSummaryResponse(
                route.id(),
                route.sourceRouteId(),
                route.directionId(),
                route.headsign(),
                route.stopIds().length,
                route.trips().size(),
                firstStopId,
                lastStopId
        );
    }

    private RaptorStopSummaryResponse toStopSummaryResponse(StopRaptor stop) {
        return new RaptorStopSummaryResponse(
                stop.id(),
                stop.name(),
                stop.routes().size(),
                stop.transfers().size()
        );
    }
}
