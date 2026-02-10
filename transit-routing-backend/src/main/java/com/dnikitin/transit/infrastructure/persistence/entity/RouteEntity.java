package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "route",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_route_city_number_type",
                        columnNames = {"city", "route_number", "vehicle_type"}
                )
        },
        indexes = {
                @Index(name = "idx_route_lookup", columnList = "city, route_number")
        }
)

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_number", nullable = false)
    private String routeNumber;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Builder.Default
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @OrderBy("stopSequence ASC")
    private List<RouteStopEntity> stops = new ArrayList<>();

    public void addRouteStop(RouteStopEntity stop) {
        stops.add(stop);
        stop.setRoute(this);
    }
}
