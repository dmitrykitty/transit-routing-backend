package com.dnikitin.transit.infrastructure.persistence.mapper;

import com.dnikitin.transit.domain.model.Route;
import com.dnikitin.transit.domain.model.Stop;
import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.RouteStopEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RouteEntityMapper {
    Route toRouteSummary(RouteEntity entity);
}
