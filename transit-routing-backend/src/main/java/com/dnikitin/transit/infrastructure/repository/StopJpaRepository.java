package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.StopEntity;
import com.dnikitin.transit.infrastructure.repository.projection.TransferCandidateRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopJpaRepository extends JpaRepository<StopEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StopEntity s WHERE s.city = :city")
    void deleteStopByCityBulk(CityEntity city);

    List<StopEntity> findAllByCity(CityEntity city);

    @Query(value = """
            select
                s1.id as fromStopId,
                s2.id as toStopId,
                cast(ceil(ST_Distance(cast(s1.location as geography), cast(s2.location as geography)) / :walkingSpeedMetersPerSecond) as integer) as durationInSeconds
            from stop s1
            join stop s2 on s1.city_id = s2.city_id and s1.id <> s2.id
            where s1.city_id = :cityId
              and ST_DWithin(cast(s1.location as geography), cast(s2.location as geography), :radiusMeters)
              and ST_Distance(cast(s1.location as geography), cast(s2.location as geography)) / :walkingSpeedMetersPerSecond <= :maxDurationSeconds
            """, nativeQuery = true)
    List<TransferCandidateRow> findTransferCandidatesByCity(
            @Param("cityId") Short cityId,
            @Param("radiusMeters") double radiusMeters,
            @Param("walkingSpeedMetersPerSecond") double walkingSpeedMetersPerSecond,
            @Param("maxDurationSeconds") int maxDurationSeconds
    );
}
