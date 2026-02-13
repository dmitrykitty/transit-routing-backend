package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.ConnectionQueryPort;
import com.dnikitin.transit.domain.model.Connection;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.repository.StopTimeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConnectionPersistenceAdapter implements ConnectionQueryPort {
    private final StopTimeJpaRepository stopTimeRepository;


    @Override
    public List<Connection> findSortedConnectionsByCityId(CityEntity city) {
        return stopTimeRepository.findSortedConnectionsByCityId(city.getId()).stream()
                .map(c -> Connection.builder()
                        .departureStopId(c.getDepStopId())
                        .arrivalStopId(c.getArrStopId())
                        .departureTime(c.getDepTime())
                        .arrivalTime(c.getArrTime())
                        .tripId(c.getTripIdExt())
                        .build())
                .toList();
    }
}
