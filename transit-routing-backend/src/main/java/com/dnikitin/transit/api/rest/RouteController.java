package com.dnikitin.transit.api.rest;

import com.dnikitin.transit.api.dto.response.RouteDetailsResponse;
import com.dnikitin.transit.api.dto.response.RouteSummaryResponse;
import com.dnikitin.transit.api.mapper.RouteDtoMapper;
import com.dnikitin.transit.application.port.in.GetRoutesUseCase;
import com.dnikitin.transit.infrastructure.persistence.entity.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final GetRoutesUseCase getRoutesUseCase;
    private final RouteDtoMapper routeMapper;

    @GetMapping("/{cityId}")
    public ResponseEntity<List<RouteSummaryResponse>> getRoutes(
            @PathVariable Short cityId,
            @RequestParam(value = "type", required = false) VehicleType type) {

        List<RouteSummaryResponse> response = (
                type == null
                        ? getRoutesUseCase.getRoutesForCity(cityId)
                        : getRoutesUseCase.getRoutesByCityAndVehicleType(cityId, type)
        ).stream()
                .map(routeMapper::toSummaryResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{cityId}/{number}")
    public ResponseEntity<RouteDetailsResponse> getRouteByCityAndRouteNumber(
            @PathVariable Short cityId,
            @PathVariable String number,
            @RequestParam("type") VehicleType type) {
        return getRoutesUseCase.getRouteByCityVehicleTypeAndRouteNumber(cityId, type, number)
                .map(route -> ResponseEntity.ok(routeMapper.toDetailsResponse(route)))
                .orElse(ResponseEntity.notFound().build());
    }
}
