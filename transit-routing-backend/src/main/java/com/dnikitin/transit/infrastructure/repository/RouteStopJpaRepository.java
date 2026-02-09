package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.RouteStopEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteStopJpaRepository extends JpaRepository<RouteStopEntity, Long> {

    @Query("""
            select rs
            from RouteStopEntity rs
            where rs.route.id in :routeIds
            order by rs.route.id, rs.stopSequence
            """)
    List<RouteStopEntity> findByRouteIdsWithStops(
            @Param("routeIds") List<Long> routeIds);
}
