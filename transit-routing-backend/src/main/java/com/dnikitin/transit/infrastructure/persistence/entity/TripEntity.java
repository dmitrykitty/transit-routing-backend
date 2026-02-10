package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(
        name = "trip",
        indexes = {
                @Index(name = "idx_trip_route", columnList = "route_id"),
                @Index(name = "idx_trip_calendar", columnList = "calendar_id"),
                @Index(name = "idx_trip_external_id", columnList = "trip_id_ext")
        }
)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal database primary key

    @Column(name = "trip_id_ext", unique = true, nullable = false)
    private String tripIdExternal; // GTFS: trip_id (unique identifier from provider)

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route; // GTFS: route_id

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ServiceCalendarEntity calendar; // GTFS: service_id

    @Column(name = "trip_headsign")
    private String headsign; // GTFS: trip_headsign (Direction shown on vehicle)

    @Column(name = "trip_short_name")
    private String shortName; // GTFS: trip_short_name

    @Column(name = "direction_id")
    private Integer directionId; // GTFS: direction_id (0=outbound, 1=inbound)

    @Column(name = "block_id")
    private String blockId; // GTFS: block_id (ID for vehicle blocks)

    @Column(name = "shape_id")
    private String shapeId; // GTFS: shape_id (Connects trip to geographical shapes)

    @Column(name = "wheelchair_accessible")
    private Integer wheelchairAccessible; // GTFS: wheelchair_accessible (0=no info, 1=yes, 2=no)

    @Column(name = "block_id")
    private String blockId; //from blocks.txt

    @Column(name = "shift")
    private String shift; // group number
}
