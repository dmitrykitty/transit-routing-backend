package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import com.dnikitin.transit.infrastructure.repository.projection.ConnectionRow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopTimeJpaRepository extends JpaRepository<StopTimeEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StopTimeEntity st WHERE st.city = :city")
    void deleteStopTimeByCityBulk(CityEntity city);

    @EntityGraph(attributePaths = {"stop", "trip"})
    List<StopTimeEntity> findAllByCity(CityEntity city);

    @EntityGraph(attributePaths = {"stop", "trip"})
    @Query("""
           select st
           from StopTimeEntity st
           where st.trip in :trips
           order by st.trip.id asc, st.stopSequence asc
           """)
    List<StopTimeEntity> findAllByTripsOrdered(@Param("trips") List<TripEntity> trips);
}

