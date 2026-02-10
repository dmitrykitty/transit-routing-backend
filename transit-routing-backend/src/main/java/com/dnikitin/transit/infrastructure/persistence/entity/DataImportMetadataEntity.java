package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_import_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataImportMetadataEntity {
    @Id
    private String fileId;

    private String cityName;

    private LocalDateTime lastImportTimestamp;

    private String lastModifiedHeader; //Last-Modified header from HTTP

}
