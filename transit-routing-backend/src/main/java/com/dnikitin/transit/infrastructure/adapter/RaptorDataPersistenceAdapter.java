package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.RaptorDataQueryPort;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RaptorDataPersistenceAdapter implements RaptorDataQueryPort {

    private final CityJpaRepository cityRepository;
    private final StopJpaRepository stopRepository;
    private final TripJpaRepository tripRepository;
    private final StopTimeJpaRepository stopTimeRepository;

    @Override
    public List<StopEntity> findStopsByCityId(Short cityId) {
        CityEntity city = getCityOrThrow(cityId);
        return stopRepository.findAllByCity(city);
    }

    @Override
    public List<TripEntity> findTripsByCityId(Short cityId) {
        CityEntity city = getCityOrThrow(cityId);
        return tripRepository.findAllByCity(city);
    }

    @Override
    public List<StopTimeEntity> findStopTimesByCityId(Short cityId) {
        CityEntity city = getCityOrThrow(cityId);
        return stopTimeRepository.findAllByCity(city);
    }

    private CityEntity getCityOrThrow(Short cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found: " + cityId));
    }
}
