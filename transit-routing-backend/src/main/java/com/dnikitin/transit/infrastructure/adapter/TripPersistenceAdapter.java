package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.TripQueryPort;
import com.dnikitin.transit.domain.model.Trip;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.persistence.mapper.TripEntityMapper;
import com.dnikitin.transit.infrastructure.repository.TripJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class TripPersistenceAdapter implements TripQueryPort {
    private final TripJpaRepository tripRepository;
    private final TripEntityMapper mapper;

    public TripPersistenceAdapter(TripJpaRepository tripRepository, TripEntityMapper mapper) {
        this.tripRepository = tripRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Trip> findByDepartureWindow(OffsetDateTime from, OffsetDateTime to) {
        return tripRepository.findTripsByTime(from, to).stream()
                .map(mapper::toTrip)
                .toList();
    }
}
