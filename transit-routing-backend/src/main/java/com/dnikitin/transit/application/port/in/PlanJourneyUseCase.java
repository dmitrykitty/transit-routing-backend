package com.dnikitin.transit.application.port.in;

import com.dnikitin.transit.domain.model.raptor.RaptorJourney;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PlanJourneyUseCase {

    List<RaptorJourney> planJourneys(
            Short cityId,
            Long sourceStopId,
            Long targetStopId,
            LocalTime departureTime
    );

    List<RaptorJourney> planJourneys(
            Short cityId,
            Long sourceStopId,
            Long targetStopId,
            LocalTime departureTime,
            LocalDate serviceDate
    );
}
