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
                        name = "uq_route_city_external_id",
                        columnNames = {"city_id", "route_id_ext"}
                )
        },
        indexes = {
                @Index(name = "idx_route_lookup", columnList = "city_id, route_number"),
                @Index(name = "idx_route_city_external", columnList = "city_id, route_id_ext"),
                @Index(name = "idx_route_city_vehicle", columnList = "city_id, vehicle_type")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal database primary key

    @Column(name = "route_id_ext", nullable = false)
    private String routeIdExternal; // GTFS: route_id (Unique within provider)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private AgencyEntity agency;

    @Column(name = "route_number", nullable = false)
    private String routeNumber; // GTFS: route_short_name (e.g., "50")

    @Column(name = "name")
    private String name; // GTFS: route_long_name (e.g., "Krowodrza Górka - Kurdwanów P+R")

//    @Column(name = "description", columnDefinition = "TEXT")
//    private String description; // GTFS: route_desc

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

//    @Column(name = "route_url")
//    private String url; // GTFS: route_url

//    @Column(name = "route_color")
//    private String color; // GTFS: route_color (hex without #)

//    @Column(name = "route_text_color")
//    private String textColor; // GTFS: route_text_color (hex without #)

    @Builder.Default
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @OrderBy("stopSequence ASC")
    private List<RouteStopEntity> stops = new ArrayList<>();

    public void addRouteStop(RouteStopEntity stop) {
        stops.add(stop);
        stop.setRoute(this);
    }
}
