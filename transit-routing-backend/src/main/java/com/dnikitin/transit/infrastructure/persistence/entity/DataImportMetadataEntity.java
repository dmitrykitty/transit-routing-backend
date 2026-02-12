package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "data_import_metadata",
        indexes = {
                @Index(name = "idx_metadata_city", columnList = "city_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataImportMetadataEntity {

    @Id
    @Column(name = "file_id")
    private String fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @Column(name = "last_import_timestamp")
    private LocalDateTime lastImportTimestamp;

    @Column(name = "last_modified_header")
    private String lastModifiedHeader;
}
