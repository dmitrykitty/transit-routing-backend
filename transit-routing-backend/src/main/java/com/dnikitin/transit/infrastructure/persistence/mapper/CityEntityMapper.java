package com.dnikitin.transit.infrastructure.persistence.mapper;

import com.dnikitin.transit.domain.model.City;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CityEntityMapper {
    City toCity(CityEntity entity);
    CityEntity toCityEntity(City city);
}
