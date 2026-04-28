package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.RouteEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.TripEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripJpaRepository extends JpaRepository<TripEntity,Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TripEntity t WHERE t.city = :city")
    void deleteTripByCityBulk(CityEntity city);

    @EntityGraph(attributePaths = {"route", "calendar"})
    List<TripEntity> findAllByCity(CityEntity city);

    @EntityGraph(attributePaths = {"route"})
    List<TripEntity> findAllByRoute(RouteEntity route);


}
