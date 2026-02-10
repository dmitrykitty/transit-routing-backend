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
                @Index(name = "idx_stop_time_stop_departure", columnList = "stop_id, departure_time"),
                @Index(name = "idx_stop_time_arrival", columnList = "arrival_time")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StopTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private StopEntity stop;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime; // Czas przyjazdu na przystanek

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime; // Czas odjazdu (uwzględnia postój)

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence; // Powtórzenie sekwencji dla optymalizacji algorytmu CSA
}