package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.Connection;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ConnectionQueryPort {
    List<Connection> findSortedConnectionsByCityId(CityEntity city);
}
