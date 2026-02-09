package com.dnikitin.transit.infrastructure.persistence.mapper;

import com.dnikitin.transit.domain.model.Trip;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripEntityMapper {
    Trip toTrip(TripEntity trip);
}
