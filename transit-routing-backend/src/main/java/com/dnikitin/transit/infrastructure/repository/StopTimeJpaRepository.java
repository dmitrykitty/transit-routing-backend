package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopTimeEntity;
import com.dnikitin.transit.infrastructure.repository.projection.ConnectionRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopTimeJpaRepository extends JpaRepository<StopTimeEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StopTimeEntity st WHERE st.city = :city")
    void deleteStopTimeByCityBulk(CityEntity city);

    List<StopTimeEntity> findAllByCity(CityEntity city);

    @Query(value = """
            SELECT
                s1.stop_id_ext AS depStopId,
                s2.stop_id_ext AS arrStopId,
                CAST(EXTRACT(EPOCH FROM st1.departure_time) AS INTEGER) AS depTimeSeconds,
                CAST(EXTRACT(EPOCH FROM st2.arrival_time) AS INTEGER) AS arrTimeSeconds,
                t.trip_id_ext AS tripIdExt
            FROM stop_time st1
            JOIN stop_time st2 ON st1.trip_id = st2.trip_id
                AND st2.stop_sequence = st1.stop_sequence + 1
            JOIN stop s1 ON st1.stop_id = s1.id
            JOIN stop s2 ON st2.stop_id = s2.id
            JOIN trip t ON st1.trip_id = t.id
            WHERE st1.city_id = :cityId
            ORDER BY st1.departure_time
            """, nativeQuery = true)
    List<ConnectionRow> findSortedConnectionsByCityId(@Param("cityId") Short cityId);

}

