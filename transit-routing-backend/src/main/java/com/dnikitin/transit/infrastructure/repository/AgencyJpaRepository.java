package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.AgencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyJpaRepository extends JpaRepository<AgencyEntity, String> {
}
