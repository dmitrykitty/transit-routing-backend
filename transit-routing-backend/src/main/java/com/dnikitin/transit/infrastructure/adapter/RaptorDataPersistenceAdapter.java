package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTransferData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.RaptorTripJdbcRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarDateJpaRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.projection.TransferCandidateRow;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RaptorDataPersistenceAdapter implements RaptorDataQueryPort {

    private final CityJpaRepository cityRepository;
    private final StopJpaRepository stopRepository;
    private final ServiceCalendarJpaRepository calendarRepository;
    private final ServiceCalendarDateJpaRepository calendarDateRepository;
    private final RaptorTripJdbcRepository tripJdbcRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RaptorStopData> findStopsByCityId(Short cityId) {
        CityEntity city = getCityOrThrow(cityId);
        return stopRepository.findAllByCity(city).stream()
                .map(this::toStopData)
                .toList();
    }

    /**
     * Selects the service calendars active on the date (all of them when the date is null) and loads only
     * their trips and stop times, as plain JDBC rows instead of JPA entities.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RaptorTripData> findTripsByCityId(Short cityId, LocalDate serviceDate) {
        CityEntity city = getCityOrThrow(cityId);
        if (serviceDate == null) {
            return tripJdbcRepository.findTripsWithStopTimes(cityId, null);
        }

        Map<String, Integer> exceptions = exceptionsByServiceId(city, serviceDate);
        List<Long> activeCalendarIds = calendarRepository.findAllByCity(city).stream()
                .filter(calendar -> isCalendarActiveOn(calendar, serviceDate, exceptions))
                .map(ServiceCalendarEntity::getId)
                .toList();

        return tripJdbcRepository.findTripsWithStopTimes(cityId, activeCalendarIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RaptorTransferData> findTransfersByCityId(
            Short cityId,
            double radiusMeters,
            double walkingSpeedMetersPerSecond,
            int maxDurationInSeconds
    ) {
        getCityOrThrow(cityId);
        return stopRepository.findTransferCandidatesByCity(
                        cityId,
                        radiusMeters,
                        walkingSpeedMetersPerSecond,
                        maxDurationInSeconds
                ).stream()
                .map(this::toTransferData)
                .toList();
    }

    private CityEntity getCityOrThrow(Short cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found: " + cityId));
    }

    private RaptorStopData toStopData(StopEntity stop) {
        Point location = stop.getLocation();
        return new RaptorStopData(
                stop.getId(),
                stop.getName(),
                location.getY(),
                location.getX()
        );
    }

    private RaptorTransferData toTransferData(TransferCandidateRow row) {
        return new RaptorTransferData(
                row.getFromStopId(),
                row.getToStopId(),
                row.getDurationInSeconds()
        );
    }

    private boolean isCalendarActiveOn(
            ServiceCalendarEntity calendar,
            LocalDate serviceDate,
            Map<String, Integer> exceptions
    ) {
        Integer exceptionType = exceptions.get(calendar.getServiceIdExternal());

        if (exceptionType != null) {
            return exceptionType == 1;
        }

        return !serviceDate.isBefore(calendar.getStartDate())
                && !serviceDate.isAfter(calendar.getEndDate())
                && isActiveWeekday(calendar, serviceDate.getDayOfWeek());
    }

    private Map<String, Integer> exceptionsByServiceId(CityEntity city, LocalDate serviceDate) {
        Map<String, Integer> result = new HashMap<>();

        for (ServiceCalendarDateEntity exception : calendarDateRepository.findAllByCity(city)) {
            if (serviceDate.equals(exception.getDate())) {
                result.merge(
                        exception.getServiceIdExternal(),
                        exception.getExceptionType(),
                        Math::max
                );
            }
        }

        return result;
    }

    private boolean isActiveWeekday(ServiceCalendarEntity calendar, DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> calendar.isMonday();
            case TUESDAY -> calendar.isTuesday();
            case WEDNESDAY -> calendar.isWednesday();
            case THURSDAY -> calendar.isThursday();
            case FRIDAY -> calendar.isFriday();
            case SATURDAY -> calendar.isSaturday();
            case SUNDAY -> calendar.isSunday();
        };
    }
}
