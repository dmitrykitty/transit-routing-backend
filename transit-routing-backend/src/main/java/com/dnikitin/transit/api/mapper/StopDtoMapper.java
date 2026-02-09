package com.dnikitin.transit.api.mapper;

import com.dnikitin.transit.api.dto.response.StopResponse;
import com.dnikitin.transit.domain.model.Stop;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StopDtoMapper {
    StopResponse toResponse(Stop stop);
}
