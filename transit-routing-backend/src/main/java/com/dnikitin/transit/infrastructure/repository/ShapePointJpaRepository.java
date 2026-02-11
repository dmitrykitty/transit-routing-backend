package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.ShapePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShapePointJpaRepository extends JpaRepository<ShapePointEntity, Long> {
}
