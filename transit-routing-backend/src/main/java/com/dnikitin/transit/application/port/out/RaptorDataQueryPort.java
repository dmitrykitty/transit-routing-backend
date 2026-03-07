package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;

import java.util.List;

public interface RaptorDataQueryPort {
    List<StopEntity> findStopsByCityId(Short cityId);

    List<TripEntity> findTripsByCityId(Short cityId);

    List<StopTimeEntity> findStopTimesByCityId(Short cityId);
}
