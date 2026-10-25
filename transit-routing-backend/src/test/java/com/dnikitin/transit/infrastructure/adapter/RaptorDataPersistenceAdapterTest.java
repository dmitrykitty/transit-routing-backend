package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.RaptorTripJdbcRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarDateJpaRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class RaptorDataPersistenceAdapterTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 27);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 4, 28);

    @Test
    void loadsOnlyTripsOfCalendarsActiveOnWeekday() {
        Fixture fixture = fixture(List.of(calendar(7L, "weekday", true, false)), List.of());

        fixture.adapter.findTripsByCityId((short) 1, MONDAY);
        fixture.adapter.findTripsByCityId((short) 1, TUESDAY);

        verify(fixture.tripJdbcRepository).findTripsWithStopTimes((short) 1, List.of(7L));
        verify(fixture.tripJdbcRepository).findTripsWithStopTimes((short) 1, List.of());
    }

    @Test
    void calendarDateRemovalExcludesCalendar() {
        Fixture fixture = fixture(
                List.of(calendar(7L, "weekday", true, true)),
                List.of(exception("weekday", MONDAY, 2))
        );

        fixture.adapter.findTripsByCityId((short) 1, MONDAY);

        verify(fixture.tripJdbcRepository).findTripsWithStopTimes((short) 1, List.of());
    }

    @Test
    void calendarDateAdditionIncludesCalendarOutsideWeekday() {
        Fixture fixture = fixture(
                List.of(calendar(7L, "special", false, false), calendar(8L, "weekday", true, false)),
                List.of(exception("special", TUESDAY, 1))
        );

        fixture.adapter.findTripsByCityId((short) 1, TUESDAY);

        verify(fixture.tripJdbcRepository).findTripsWithStopTimes((short) 1, List.of(7L));
    }

    @Test
    void loadsTripsOfAllCalendarsWithoutServiceDate() {
        Fixture fixture = fixture(List.of(calendar(7L, "weekday", true, false)), List.of());
        List<RaptorTripData> trips = List.of(new RaptorTripData(1, 100, 0, "Bravo", List.of()));
        when(fixture.tripJdbcRepository.findTripsWithStopTimes((short) 1, null)).thenReturn(trips);

        assertSame(trips, fixture.adapter.findTripsByCityId((short) 1, null));
        verifyNoInteractions(fixture.calendarRepository);
    }

    private Fixture fixture(List<ServiceCalendarEntity> calendars, List<ServiceCalendarDateEntity> exceptions) {
        CityJpaRepository cityRepository = mock(CityJpaRepository.class);
        ServiceCalendarJpaRepository calendarRepository = mock(ServiceCalendarJpaRepository.class);
        ServiceCalendarDateJpaRepository calendarDateRepository = mock(ServiceCalendarDateJpaRepository.class);
        RaptorTripJdbcRepository tripJdbcRepository = mock(RaptorTripJdbcRepository.class);

        CityEntity city = CityEntity.builder()
                .id((short) 1)
                .name("Krakow")
                .build();

        when(cityRepository.findById((short) 1)).thenReturn(Optional.of(city));
        when(calendarRepository.findAllByCity(city)).thenReturn(calendars);
        when(calendarDateRepository.findAllByCity(city)).thenReturn(exceptions);

        return new Fixture(
                calendarRepository,
                tripJdbcRepository,
                new RaptorDataPersistenceAdapter(
                        cityRepository,
                        mock(StopJpaRepository.class),
                        calendarRepository,
                        calendarDateRepository,
                        tripJdbcRepository
                )
        );
    }

    private ServiceCalendarEntity calendar(Long id, String serviceId, boolean monday, boolean tuesday) {
        return ServiceCalendarEntity.builder()
                .id(id)
                .serviceIdExternal(serviceId)
                .monday(monday)
                .tuesday(tuesday)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
    }

    private ServiceCalendarDateEntity exception(String serviceId, LocalDate date, int exceptionType) {
        return ServiceCalendarDateEntity.builder()
                .serviceIdExternal(serviceId)
                .date(date)
                .exceptionType(exceptionType)
                .build();
    }

    private record Fixture(
            ServiceCalendarJpaRepository calendarRepository,
            RaptorTripJdbcRepository tripJdbcRepository,
            RaptorDataPersistenceAdapter adapter
    ) {
    }
}
