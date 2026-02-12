package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteJpaRepository extends JpaRepository<RouteEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RouteEntity r WHERE r.city = :city")
    void deleteRouteByCityBulk(String city);

    List<RouteEntity> findAllByCity(String city);
}
