package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(
        name = "route_stop",
        uniqueConstraints = {
                // Kluczowe: trasa i sekwencja muszą być unikalne
                @UniqueConstraint(name = "uq_route_stop_route_sequence", columnNames = {"route_id", "stop_sequence"})
        },
        indexes = {
                @Index(name = "idx_route_stop_route", columnList = "route_id"),
                @Index(name = "idx_route_stop_stop", columnList = "stop_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RouteStopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private StopEntity stop;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    // --- GTFS ---

    @Column(name = "pickup_type")
    @Builder.Default
    private Integer pickupType = 0; // 0=regular, 1=no pickup

    @Column(name = "drop_off_type")
    @Builder.Default
    private Integer dropOffType = 0; // 0=regular, 1=no drop-off

    @Column(name = "timepoint")
    private Integer timepoint; // 1=exact, 0=approximate

    @Column(name = "shape_dist_traveled")
    private Double shapeDistTraveled; // dystans od początku trasy w metrach
}
