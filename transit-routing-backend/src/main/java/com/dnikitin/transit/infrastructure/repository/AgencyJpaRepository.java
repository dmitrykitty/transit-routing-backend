package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.AgencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgencyJpaRepository extends JpaRepository<AgencyEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AgencyEntity a WHERE a.city = :city")
    void deleteAgencyByCityBulk(String city);

    List<AgencyEntity> findAllByCity(String city);
}
