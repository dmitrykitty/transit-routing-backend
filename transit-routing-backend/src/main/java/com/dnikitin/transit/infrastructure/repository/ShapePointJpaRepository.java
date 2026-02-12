package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ShapePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShapePointJpaRepository extends JpaRepository<ShapePointEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ShapePointEntity e WHERE e.city = :city")
    void deleteShapePointByCityBulk(CityEntity city);

    List<ShapePointEntity> findAllByCity(CityEntity city);
}
