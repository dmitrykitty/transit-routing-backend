package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StopTimeJpaRepository extends JpaRepository<StopTimeEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StopTimeEntity st WHERE st.city = :city")
    void deleteStopTimeByCityBulk(String cityName);

    List<StopTimeEntity> findAllByCity(String city);
}

