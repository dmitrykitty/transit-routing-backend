package com.dnikitin.transit.api.mapper;

import com.dnikitin.transit.api.dto.response.CityResponse;
import com.dnikitin.transit.domain.model.City;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CityDtoMapper {
    CityResponse toCityResponse(City city);
}
