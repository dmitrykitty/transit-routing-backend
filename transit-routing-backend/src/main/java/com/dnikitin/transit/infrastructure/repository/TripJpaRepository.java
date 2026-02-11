package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TripJpaRepository extends JpaRepository<TripEntity,Long> {
    List<TripEntity> findTripsByTime(
            @Param("timeFrom") OffsetDateTime timeFrom,
            @Param("timeTo") OffsetDateTime timeTo);

    @Query("SELECT TripEntity FROM TripEntity t WHERE t.city = :city")
    List<TripEntity> findTripsByCity(String city);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TripEntity t WHERE t.city = :cityName")
    void deleteTripByCityBulk(String cityName);


}
