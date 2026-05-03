package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarDateJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RaptorDataPersistenceAdapterTest {

    @Test
    void filtersTripsByCalendarWeekday() {
        Fixture fixture = fixture();
        TripEntity trip = trip(1L, calendar("weekday", true, false));

        when(fixture.tripRepository.findAllByCity(fixture.city)).thenReturn(List.of(trip));
        when(fixture.calendarDateRepository.findAllByCity(fixture.city)).thenReturn(List.of());
        when(fixture.stopTimeRepository.findAllByCity(fixture.city)).thenReturn(List.of());

        assertEquals(1, fixture.adapter.findTripsByCityId((short) 1, LocalDate.of(2026, 4, 27)).size());
        assertEquals(0, fixture.adapter.findTripsByCityId((short) 1, LocalDate.of(2026, 4, 28)).size());
    }

    @Test
    void calendarDateRemovalExcludesTrip() {
        Fixture fixture = fixture();
        TripEntity trip = trip(1L, calendar("weekday", true, true));

        when(fixture.tripRepository.findAllByCity(fixture.city)).thenReturn(List.of(trip));
        when(fixture.calendarDateRepository.findAllByCity(fixture.city)).thenReturn(List.of(
                exception("weekday", LocalDate.of(2026, 4, 27), 2)
        ));
        when(fixture.stopTimeRepository.findAllByCity(fixture.city)).thenReturn(List.of());

        assertEquals(0, fixture.adapter.findTripsByCityId((short) 1, LocalDate.of(2026, 4, 27)).size());
    }

    @Test
    void calendarDateAdditionIncludesTripOutsideWeekday() {
        Fixture fixture = fixture();
        TripEntity trip = trip(1L, calendar("special", false, false));

        when(fixture.tripRepository.findAllByCity(fixture.city)).thenReturn(List.of(trip));
        when(fixture.calendarDateRepository.findAllByCity(fixture.city)).thenReturn(List.of(
                exception("special", LocalDate.of(2026, 4, 28), 1)
        ));
        when(fixture.stopTimeRepository.findAllByCity(fixture.city)).thenReturn(List.of());

        assertEquals(1, fixture.adapter.findTripsByCityId((short) 1, LocalDate.of(2026, 4, 28)).size());
    }

    private Fixture fixture() {
        CityJpaRepository cityRepository = mock(CityJpaRepository.class);
        StopJpaRepository stopRepository = mock(StopJpaRepository.class);
        TripJpaRepository tripRepository = mock(TripJpaRepository.class);
        StopTimeJpaRepository stopTimeRepository = mock(StopTimeJpaRepository.class);
        ServiceCalendarDateJpaRepository calendarDateRepository = mock(ServiceCalendarDateJpaRepository.class);

        CityEntity city = CityEntity.builder()
                .id((short) 1)
                .name("Krakow")
                .build();

        when(cityRepository.findById((short) 1)).thenReturn(Optional.of(city));

        return new Fixture(
                city,
                tripRepository,
                stopTimeRepository,
                calendarDateRepository,
                new RaptorDataPersistenceAdapter(
                        cityRepository,
                        stopRepository,
                        tripRepository,
                        stopTimeRepository,
                        calendarDateRepository
                )
        );
    }

    private TripEntity trip(Long id, ServiceCalendarEntity calendar) {
        return TripEntity.builder()
                .id(id)
                .route(RouteEntity.builder().id(100L).build())
                .calendar(calendar)
                .build();
    }

    private ServiceCalendarEntity calendar(String serviceId, boolean monday, boolean tuesday) {
        return ServiceCalendarEntity.builder()
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
            CityEntity city,
            TripJpaRepository tripRepository,
            StopTimeJpaRepository stopTimeRepository,
            ServiceCalendarDateJpaRepository calendarDateRepository,
            RaptorDataPersistenceAdapter adapter
    ) {
    }
}
