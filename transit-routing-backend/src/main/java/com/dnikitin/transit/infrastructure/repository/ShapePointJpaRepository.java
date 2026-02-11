package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.ShapePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ShapePointJpaRepository extends JpaRepository<ShapePointEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ShapePointEntity e WHERE e.city = :cityName")
    void deleteShapePointByCityBulk(String cityName);
}
