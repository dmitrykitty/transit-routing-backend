package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.in.BuildRaptorDataUseCase;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.service.RaptorRouter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class RaptorJourneyPlanningServiceTest {

    @Test
    void delegatesToDatasetBuilderAndDomainRouter() {
        BuildRaptorDataUseCase buildRaptorDataUseCase = mock(BuildRaptorDataUseCase.class);
        RaptorRouter raptorRouter = mock(RaptorRouter.class);
        RaptorDataSet dataSet = mock(RaptorDataSet.class);
        List<RaptorJourney> expected = List.of(mock(RaptorJourney.class));

        when(buildRaptorDataUseCase.buildForCity((short) 7, null)).thenReturn(dataSet);
        when(raptorRouter.findJourneys(dataSet, 11, 22, LocalTime.of(8, 30))).thenReturn(expected);

        RaptorJourneyPlanningService service = new RaptorJourneyPlanningService(
                buildRaptorDataUseCase,
                raptorRouter
        );

        List<RaptorJourney> actual = service.planJourneys((short) 7, 11L, 22L, LocalTime.of(8, 30));

        assertSame(expected, actual);
        verify(buildRaptorDataUseCase).buildForCity((short) 7, null);
        verify(raptorRouter).findJourneys(dataSet, 11, 22, LocalTime.of(8, 30));
        verifyNoMoreInteractions(buildRaptorDataUseCase, raptorRouter);
    }

    @Test
    void delegatesServiceDateToDatasetBuilder() {
        BuildRaptorDataUseCase buildRaptorDataUseCase = mock(BuildRaptorDataUseCase.class);
        RaptorRouter raptorRouter = mock(RaptorRouter.class);
        RaptorDataSet dataSet = mock(RaptorDataSet.class);
        LocalDate serviceDate = LocalDate.of(2026, 4, 28);

        when(buildRaptorDataUseCase.buildForCity((short) 7, serviceDate)).thenReturn(dataSet);

        RaptorJourneyPlanningService service = new RaptorJourneyPlanningService(
                buildRaptorDataUseCase,
                raptorRouter
        );

        service.planJourneys((short) 7, 11L, 22L, LocalTime.of(8, 30), serviceDate);

        verify(buildRaptorDataUseCase).buildForCity((short) 7, serviceDate);
        verify(raptorRouter).findJourneys(dataSet, 11, 22, LocalTime.of(8, 30));
    }
}
