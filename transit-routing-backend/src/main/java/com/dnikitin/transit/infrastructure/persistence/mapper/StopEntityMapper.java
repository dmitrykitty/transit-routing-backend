package com.dnikitin.transit.infrastructure.persistence.mapper;

import com.dnikitin.transit.domain.model.Stop;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface StopEntityMapper {

    @Mapping(target = "lat", source = "location",qualifiedByName = "latFromPoint")
    @Mapping(target = "lon", source = "location",qualifiedByName = "lonFromPoint")
    Stop toStop(StopEntity stopEntity);

    @Named("latFromPoint")
    default double lat(Point p) { return p.getY(); } // lat = Y
    @Named("lonFromPoint")
    default double lon(Point p) { return p.getX(); } // lon = X
}
