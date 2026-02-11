package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agency")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyEntity {

    @Id
    @Column(name = "agency_id_ext")
    private String agencyIdExternal; // GTFS: agency_id (np. "MPK")

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

    @Column(name = "agency_fare_url")
    private String fareUrl; // GTFS: agency_fare_url

    @Column(name = "agency_email")
    private String email; // GTFS: agency_email
}
