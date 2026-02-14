package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "agency",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_agency_city_external_id", columnNames = {"city_id", "agency_id_ext"})
        },
        indexes = {
                @Index(name = "idx_agency_city_external", columnList = "city_id, agency_id_ext"),
                @Index(name = "idx_agency_city_name", columnList = "city_id, agency_name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agency_id_ext", nullable = false)
    private String agencyIdExternal; // GTFS: agency_id (np. "MPK")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @Column(name = "agency_name", nullable = false)
    private String name; // GTFS: agency_name

    @Column(name = "agency_url", nullable = false)
    private String url; // GTFS: agency_url

    @Column(name = "agency_timezone", nullable = false)
    private String timezone; // GTFS: agency_timezone (np. "Europe/Warsaw")

    @Column(name = "agency_lang")
    private String lang; // GTFS: agency_lang

    @Column(name = "agency_phone")
    private String phone; // GTFS: agency_phone

//    @Column(name = "agency_fare_url")
//    private String fareUrl; // GTFS: agency_fare_url

    @Column(name = "agency_email")
    private String email; // GTFS: agency_email
}
