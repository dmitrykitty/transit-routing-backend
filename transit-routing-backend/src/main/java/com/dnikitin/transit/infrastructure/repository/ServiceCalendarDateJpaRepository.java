package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCalendarDateJpaRepository extends JpaRepository<ServiceCalendarDateEntity, Long> {
}
