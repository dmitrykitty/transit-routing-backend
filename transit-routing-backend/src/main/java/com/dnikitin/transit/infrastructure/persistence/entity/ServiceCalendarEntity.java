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
        indexes = {
                @Index(name = "idx_calendar_external_id", columnList = "service_id_ext")
        }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Wewnętrzny klucz główny (PK)

    @Column(name = "service_id_ext", unique = true, nullable = false)
    private String serviceIdExternal; // mapping of service_id (np. "service_1")

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