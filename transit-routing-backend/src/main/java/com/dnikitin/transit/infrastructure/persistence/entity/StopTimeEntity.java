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
        indexes = {
                @Index(name = "idx_stop_time_trip", columnList = "trip_id"),
                @Index(name = "idx_stop_time_stop", columnList = "stop_id"),
                @Index(name = "idx_stop_time_departure", columnList = "departure_time")
        }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StopTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip; // trip_id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private StopEntity stop; // stop_id

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime; // arrival_time

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime; // departure_time

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence; // stop_sequence

    @Column(name = "stop_headsign")
    private String stopHeadsign; // stop_headsign (opcjonalny tekst na wyświetlaczu dla tego przystanku)

    @Column(name = "pickup_type")
    private Integer pickupType; // pickup_type (0=regular, 1 = no pickup)

    @Column(name = "drop_off_type")
    private Integer dropOffType; // drop_off_type (0=regular, 1=no drop-off)

    @Column(name = "shape_dist_traveled")
    private Double shapeDistTraveled; // shape_dist_traveled (metres from the beggining of the route)

    @Column(name = "timepoint")
    private Integer timepoint;
}