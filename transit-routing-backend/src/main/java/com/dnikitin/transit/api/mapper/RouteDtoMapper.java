package com.dnikitin.transit.api.mapper;

import com.dnikitin.transit.api.dto.response.RouteDetailsResponse;
import com.dnikitin.transit.api.dto.response.RouteSummaryResponse;
import com.dnikitin.transit.domain.model.Route;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RouteDtoMapper {
    RouteDetailsResponse toDetailsResponse(Route route);
    RouteSummaryResponse toSummaryResponse(Route route);
}
