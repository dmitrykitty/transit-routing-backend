package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "trip",
        indexes = {
                @Index(name = "idx_trip_route", columnList = "route_id"),
                @Index(name = "idx_trip_calendar", columnList = "calendar_id")
        }
)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ServiceCalendarEntity calendar;

    @Column(name = "headsign")
    private String headsign; // "Kierunek: Lotnisko"

    @Column(name = "direction_id")
    private Integer directionId; // 0 for one way, 1 for reverse
}
