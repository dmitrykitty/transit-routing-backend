package com.dnikitin.transit.api.mapper;

import com.dnikitin.transit.api.dto.response.RouteDetailsResponse;
import com.dnikitin.transit.api.dto.response.RouteDirectionResponse;
import com.dnikitin.transit.api.dto.response.RouteSummaryResponse;
import com.dnikitin.transit.api.dto.response.StopResponse;
import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.domain.model.RouteDetails;
import com.dnikitin.transit.domain.model.RouteDirection;
import com.dnikitin.transit.domain.model.Stop;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RouteDtoMapper {
    RouteSummaryResponse toSummaryResponse(Route route);

    RouteDetailsResponse toDetailsResponse(RouteDetails routeDetails);

    RouteDirectionResponse toDirectionResponse(RouteDirection direction);

    StopResponse toStopResponse(Stop stop);
}
