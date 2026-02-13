package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteStopJpaRepository extends JpaRepository<RouteStopEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RouteStopEntity rs WHERE rs.city = :city")
    void deleteRouteStopByCityBulk(CityEntity city);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO route_stop (city_id, route_id, stop_id, stop_sequence, pickup_type, drop_off_type, timepoint, shape_dist_traveled)
            SELECT DISTINCT ON (r.id, st.stop_sequence)
                r.city_id, r.id, st.stop_id, st.stop_sequence, st.pickup_type, st.drop_off_type, st.timepoint, st.shape_dist_traveled
            FROM route r
            JOIN trip t ON t.route_id = r.id
            JOIN stop_time st ON st.trip_id = t.id
            WHERE r.city_id = :cityId
            """, nativeQuery = true)
    void insertRouteStopByCity(@Param("cityId") Short cityId);

    List<RouteStopEntity> findAllByCity(CityEntity city);
}
