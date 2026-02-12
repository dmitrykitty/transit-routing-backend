package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.ServiceCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceCalendarJpaRepository extends JpaRepository<ServiceCalendarEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ServiceCalendarEntity sc WHERE sc.city = :city")
    void deleteServiceCalendarByCityBulk(CityEntity city);

    List<ServiceCalendarEntity> findAllByCity(CityEntity city);
}
