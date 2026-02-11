package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCalendarJpaRepository extends JpaRepository<ServiceCalendarEntity, Integer> {
}
