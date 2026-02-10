package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "service_calendar")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Flagi dla dni tygodnia
    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;
    private boolean saturday;
    private boolean sunday;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Od kiedy obowiązuje ten rozkład

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;   // Do kiedy obowiązuje
}