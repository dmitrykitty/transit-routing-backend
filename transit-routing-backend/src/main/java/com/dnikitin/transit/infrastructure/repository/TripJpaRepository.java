package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TripJpaRepository extends JpaRepository<TripEntity,Long> {
    @Query("""
            select t
            from TripEntity t
            where :timeFrom < t.departureTime and
                  t.arrivalTime < :timeTo
            order by t.departureTime asc
            """
    )
    List<TripEntity> findTripsByTime(
            @Param("timeFrom") OffsetDateTime timeFrom,
            @Param("timeTo") OffsetDateTime timeTo);
}
