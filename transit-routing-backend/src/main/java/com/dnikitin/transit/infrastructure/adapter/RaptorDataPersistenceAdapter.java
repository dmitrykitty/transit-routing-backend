package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopData;
import com.dnikitin.transit.application.port.out.raptor.RaptorStopTimeData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTransferData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.ServiceCalendarDateJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import com.dnikitin.transit.infrastructure.repository.projection.TransferCandidateRow;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RaptorDataPersistenceAdapter implements RaptorDataQueryPort {

    private final CityJpaRepository cityRepository;
    private final StopJpaRepository stopRepository;
    private final TripJpaRepository tripRepository;
    private final StopTimeJpaRepository stopTimeRepository;
    private final ServiceCalendarDateJpaRepository calendarDateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RaptorStopData> findStopsByCityId(Short cityId) {
        CityEntity city = getCityOrThrow(cityId);
        return stopRepository.findAllByCity(city).stream()
                .map(this::toStopData)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RaptorTripData> findTripsByCityId(Short cityId, LocalDate serviceDate) {
        CityEntity city = getCityOrThrow(cityId);
        Map<String, Integer> exceptions = serviceDate == null
                ? Map.of()
                : exceptionsByServiceId(city, serviceDate);
        List<TripEntity> trips = tripRepository.findAllByCity(city).stream()
                .filter(trip -> serviceDate == null || isTripActiveOn(trip, serviceDate, exceptions))
                .toList();

        List<StopTimeEntity> stopTimes = stopTimeRepository.findAllByCity(city);
        Map<Long, List<StopTimeEntity>> stopTimesByTripId = groupStopTimesByTrip(stopTimes);

        return trips.stream()
                .map(trip -> toTripData(trip, stopTimesByTripId.getOrDefault(trip.getId(), List.of())))
                .toList();
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

    private RaptorTripData toTripData(TripEntity trip, List<StopTimeEntity> stopTimes) {
        return new RaptorTripData(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getDirectionId(),
                trip.getHeadsign(),
                stopTimes.stream()
                        .sorted(Comparator.comparingInt(StopTimeEntity::getStopSequence))
                        .map(this::toStopTimeData)
                        .toList()
        );
    }

    private RaptorStopTimeData toStopTimeData(StopTimeEntity stopTime) {
        return new RaptorStopTimeData(
                stopTime.getStop().getId(),
                stopTime.getStop().getName(),
                stopTime.getArrivalTime(),
                stopTime.getDepartureTime(),
                stopTime.getStopSequence()
        );
    }

    private RaptorTransferData toTransferData(TransferCandidateRow row) {
        return new RaptorTransferData(
                row.getFromStopId(),
                row.getToStopId(),
                row.getDurationInSeconds()
        );
    }

    private Map<Long, List<StopTimeEntity>> groupStopTimesByTrip(List<StopTimeEntity> stopTimes) {
        Map<Long, List<StopTimeEntity>> result = new HashMap<>();

        for (StopTimeEntity stopTime : stopTimes) {
            result.computeIfAbsent(stopTime.getTrip().getId(), ignored -> new ArrayList<>())
                    .add(stopTime);
        }

        return result;
    }

    private boolean isTripActiveOn(
            TripEntity trip,
            LocalDate serviceDate,
            Map<String, Integer> exceptions
    ) {
        ServiceCalendarEntity calendar = trip.getCalendar();
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
