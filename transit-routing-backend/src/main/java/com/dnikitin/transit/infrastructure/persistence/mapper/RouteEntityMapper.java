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
public abstract class RouteEntityMapper {

    @Autowired
    protected StopEntityMapper stopEntityMapper;

    @Mapping(target = "stops", expression = "java(mapStops(entity.getStops()))")
    public abstract Route toRoute(RouteEntity entity);

    public abstract RouteEntity toRouteEntity(Route route);

    protected List<Stop> mapStops(List<RouteStopEntity> routeStops) {
        if (routeStops == null) return List.of();
        return routeStops.stream()
                .sorted((a, b) -> Integer.compare(a.getStopSequence(), b.getStopSequence()))
                .map(rs -> stopEntityMapper.toStop(rs.getStop()))
                .toList();
    }
}
