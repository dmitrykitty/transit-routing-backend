package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteStopJpaRepository extends JpaRepository<RouteStopEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RouteStopEntity rs WHERE rs.city = :city")
    void deleteRouteStopByCityBulk(String city);

    List<RouteStopEntity> findAllByCity(String city);
}
