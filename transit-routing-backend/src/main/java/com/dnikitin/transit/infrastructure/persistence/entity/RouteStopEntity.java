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
                @UniqueConstraint(
                        name = "uq_route_stop_route_sequence_city",
                        columnNames = {"route_id", "stop_sequence", "city"}
                )
        },
        indexes = {
                @Index(name = "idx_route_stop_route", columnList = "route_id"),
                @Index(name = "idx_route_stop_stop", columnList = "stop_id"),
                @Index(name = "idx_route_stop_city", columnList = "city")
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

    @Column(nullable = false)
    private String city; // Denormalizacja dla wydajności i izolacji

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private StopEntity stop;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    // --- GTFS / Operational Data ---

    @Column(name = "pickup_type")
    @Builder.Default
    private Integer pickupType = 0;

    @Column(name = "drop_off_type")
    @Builder.Default
    private Integer dropOffType = 0;

    @Column(name = "timepoint")
    private Integer timepoint;

    @Column(name = "shape_dist_traveled")
    private Double shapeDistTraveled;
}
