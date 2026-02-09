package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.StopSearchPort;
import com.dnikitin.transit.domain.model.Stop;
import com.dnikitin.transit.infrastructure.persistence.mapper.StopEntityMapper;
import com.dnikitin.transit.infrastructure.repository.StopJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StopPersistenceAdapter implements StopSearchPort {

    private final StopJpaRepository repository;
    private final StopEntityMapper mapper;

    public StopPersistenceAdapter(StopJpaRepository repository, StopEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Stop> getStops() {
        return repository.findAll().stream()
                .map(mapper::toStop)
                .toList();
    }
}
