package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.in.BuildRaptorDataUseCase;
import com.dnikitin.transit.application.port.in.PlanJourneyUseCase;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.service.RaptorRouter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RaptorJourneyPlanningService implements PlanJourneyUseCase {

    private final BuildRaptorDataUseCase buildRaptorDataUseCase;
    private final RaptorRouter raptorRouter;

    public RaptorJourneyPlanningService(
            BuildRaptorDataUseCase buildRaptorDataUseCase,
            RaptorRouter raptorRouter
    ) {
        this.buildRaptorDataUseCase = buildRaptorDataUseCase;
        this.raptorRouter = raptorRouter;
    }

    @Override
    public List<RaptorJourney> planJourneys(
            Short cityId,
            Long sourceStopId,
            Long targetStopId,
            LocalTime departureTime
    ) {
        return planJourneys(cityId, sourceStopId, targetStopId, departureTime, null);
    }

    @Override
    public List<RaptorJourney> planJourneys(
            Short cityId,
            Long sourceStopId,
            Long targetStopId,
            LocalTime departureTime,
            LocalDate serviceDate
    ) {
        RaptorDataSet dataSet = buildRaptorDataUseCase.buildForCity(cityId, serviceDate);

        return raptorRouter.findJourneys(
                dataSet,
                Math.toIntExact(sourceStopId),
                Math.toIntExact(targetStopId),
                departureTime
        );
    }
}
