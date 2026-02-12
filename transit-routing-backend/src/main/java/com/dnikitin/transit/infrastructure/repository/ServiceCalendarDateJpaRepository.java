package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceCalendarDateJpaRepository extends JpaRepository<ServiceCalendarDateEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ServiceCalendarDateEntity scd WHERE scd.city = :city")
    void deleteServiceCalendarDateByCityBulk(String city);

    List<ServiceCalendarDateEntity> findAllByCity(String city);
}
