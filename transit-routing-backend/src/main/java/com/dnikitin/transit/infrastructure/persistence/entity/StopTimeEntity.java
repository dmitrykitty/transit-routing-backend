package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(
        name = "stop_time",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_stop_time_trip_sequence_city",
                        columnNames = {"trip_id", "stop_sequence", "city_id"}
                )
        },
        indexes = {
                @Index(name = "idx_stop_time_trip", columnList = "trip_id"),
                @Index(name = "idx_stop_time_stop", columnList = "stop_id"),
                @Index(name = "idx_stop_time_departure", columnList = "departure_time"),
                @Index(name = "idx_stop_time_city", columnList = "city_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private StopEntity stop;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "stop_headsign")
    private String stopHeadsign;

    @Column(name = "pickup_type")
    private Integer pickupType;

    @Column(name = "drop_off_type")
    private Integer dropOffType;

    @Column(name = "shape_dist_traveled")
    private Double shapeDistTraveled;

    @Column(name = "timepoint")
    private Integer timepoint;
}