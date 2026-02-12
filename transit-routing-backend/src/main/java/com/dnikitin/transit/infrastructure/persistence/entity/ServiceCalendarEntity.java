package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "service_calendar",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_calendar_city_external_id", columnNames = {"city_id", "service_id_ext"})
        },
        indexes = {
                @Index(name = "idx_calendar_city_external", columnList = "city_id, service_id_ext"),
                @Index(name = "idx_calendar_city_range", columnList = "city_id, start_date, end_date")
        }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Wewnętrzny klucz główny (PK)

    @Column(name = "service_id_ext", nullable = false)
    private String serviceIdExternal; // mapping of service_id (np. "service_1")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;
    private boolean saturday;
    private boolean sunday;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Mapping 20260207

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;   // Mapping 20260508
}
