package com.dnikitin.transit.infrastructure.persistence.entity;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "stop",
        indexes = {
                @Index(name = "idx_stop_location", columnList = "location"),
                @Index(name = "idx_stop_external_id", columnList = "stop_id_ext")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal database primary key

    @Column(name = "stop_id_ext", unique = true, nullable = false)
    private String stopIdExternal; // GTFS: stop_id (unique identifier from provider)

    @Column(name = "stop_code")
    private String stopCode; // GTFS: stop_code (short text identifier for passengers)

    @Column(nullable = false)
    private String name; // GTFS: stop_name

    @Column(columnDefinition = "TEXT")
    private String description; // GTFS: stop_desc

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location; // Mapping GTFS: stop_lat and stop_lon into JTS Point

    @Column(name = "zone_id")
    private String zoneId; // GTFS: zone_id (for fare calculation)

    @Column(name = "stop_url")
    private String url; // GTFS: stop_url

    @Column(name = "location_type")
    private Integer locationType; // GTFS: location_type (0=stop, 1=station, 2=entrance, etc.)

    @Column(name = "parent_station")
    private String parentStation; // GTFS: parent_station (ID of the parent hub)

    @Column(name = "stop_timezone")
    private String timezone; // GTFS: stop_timezone

    @Column(name = "wheelchair_boarding")
    private Integer wheelchairBoarding; // GTFS: wheelchair_boarding (0=no info, 1=yes, 2=no)
}
