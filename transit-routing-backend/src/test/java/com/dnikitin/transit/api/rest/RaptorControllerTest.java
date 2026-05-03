package com.dnikitin.transit.api.rest;

import com.dnikitin.transit.api.dto.response.RaptorDataSetSummaryResponse;
import com.dnikitin.transit.api.dto.response.RaptorJourneyResponse;
import com.dnikitin.transit.api.mapper.RaptorDtoMapper;
import com.dnikitin.transit.application.port.in.BuildRaptorDataUseCase;
import com.dnikitin.transit.application.port.in.PlanJourneyUseCase;
import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RaptorControllerTest {

    @Test
    void planJourneysReturnsMappedResponse() {
        PlanJourneyUseCase planJourneyUseCase = mock(PlanJourneyUseCase.class);
        BuildRaptorDataUseCase buildRaptorDataUseCase = mock(BuildRaptorDataUseCase.class);
        RaptorDtoMapper mapper = mock(RaptorDtoMapper.class);
        RaptorJourney journey = mock(RaptorJourney.class);
        RaptorJourneyResponse response = mock(RaptorJourneyResponse.class);

        when(planJourneyUseCase.planJourneys((short) 1, 10L, 20L, LocalTime.of(9, 15), null))
                .thenReturn(List.of(journey));
        when(mapper.toJourneyResponse(journey)).thenReturn(response);

        RaptorController controller = new RaptorController(
                planJourneyUseCase,
                buildRaptorDataUseCase,
                mapper
        );

        List<RaptorJourneyResponse> body = controller.planJourneys(
                (short) 1,
                10L,
                20L,
                LocalTime.of(9, 15),
                null
        ).getBody();

        assertEquals(List.of(response), body);
    }

    @Test
    void planJourneysPassesServiceDateWhenProvided() {
        PlanJourneyUseCase planJourneyUseCase = mock(PlanJourneyUseCase.class);
        BuildRaptorDataUseCase buildRaptorDataUseCase = mock(BuildRaptorDataUseCase.class);
        RaptorDtoMapper mapper = mock(RaptorDtoMapper.class);
        LocalDate serviceDate = LocalDate.of(2026, 4, 28);

        RaptorController controller = new RaptorController(
                planJourneyUseCase,
                buildRaptorDataUseCase,
                mapper
        );

        controller.planJourneys(
                (short) 1,
                10L,
                20L,
                LocalTime.of(9, 15),
                serviceDate
        );

        verify(planJourneyUseCase).planJourneys(
                (short) 1,
                10L,
                20L,
                LocalTime.of(9, 15),
                serviceDate
        );
    }

    @Test
    void getDatasetReturnsMappedSummary() {
        PlanJourneyUseCase planJourneyUseCase = mock(PlanJourneyUseCase.class);
        BuildRaptorDataUseCase buildRaptorDataUseCase = mock(BuildRaptorDataUseCase.class);
        RaptorDtoMapper mapper = mock(RaptorDtoMapper.class);
        RaptorDataSet dataSet = mock(RaptorDataSet.class);
        RaptorDataSetSummaryResponse response = mock(RaptorDataSetSummaryResponse.class);

        when(buildRaptorDataUseCase.buildForCity((short) 2)).thenReturn(dataSet);
        when(mapper.toDataSetSummaryResponse((short) 2, dataSet, 15, 10)).thenReturn(response);

        RaptorController controller = new RaptorController(
                planJourneyUseCase,
                buildRaptorDataUseCase,
                mapper
        );

        RaptorDataSetSummaryResponse body = controller.getRaptorDataSet((short) 2, 15, 10).getBody();

        assertEquals(response, body);
    }
}
