package com.dnikitin.transit.api.rest;

import com.dnikitin.transit.api.dto.response.RaptorDataSetSummaryResponse;
import com.dnikitin.transit.api.dto.response.RaptorJourneyResponse;
import com.dnikitin.transit.api.mapper.RaptorDtoMapper;
import com.dnikitin.transit.application.port.in.BuildRaptorDataUseCase;
import com.dnikitin.transit.application.port.in.PlanJourneyUseCase;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/raptor")
@RequiredArgsConstructor
@Validated
public class RaptorController {

    private final PlanJourneyUseCase planJourneyUseCase;
    private final BuildRaptorDataUseCase buildRaptorDataUseCase;
    private final RaptorDtoMapper raptorDtoMapper;

    @GetMapping("/cities/{cityId}/journeys")
    public ResponseEntity<List<RaptorJourneyResponse>> planJourneys(
            @PathVariable Short cityId,
            @RequestParam Long sourceStopId,
            @RequestParam Long targetStopId,
            @RequestParam LocalTime departureTime,
            @RequestParam(required = false) LocalDate serviceDate
    ) {
        List<RaptorJourneyResponse> response = planJourneyUseCase.planJourneys(
                        cityId,
                        sourceStopId,
                        targetStopId,
                        departureTime,
                        serviceDate
                ).stream()
                .map(raptorDtoMapper::toJourneyResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cities/{cityId}/dataset")
    public ResponseEntity<RaptorDataSetSummaryResponse> getRaptorDataSet(
            @PathVariable Short cityId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int routeLimit,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int stopLimit
    ) {
        RaptorDataSet dataSet = buildRaptorDataUseCase.buildForCity(cityId);
        return ResponseEntity.ok(
                raptorDtoMapper.toDataSetSummaryResponse(cityId, dataSet, routeLimit, stopLimit)
        );
    }
}
