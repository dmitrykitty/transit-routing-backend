package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.infrastructure.persistence.entity.DataImportMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DataImportMetadataJpaRepository extends JpaRepository<DataImportMetadataEntity, String> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DataImportMetadataEntity dim WHERE dim.city = :cityName")
    void deleteMetaDataByCityBulk(String cityName);
}
