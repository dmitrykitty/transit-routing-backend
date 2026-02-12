package com.dnikitin.transit.infrastructure.persistence.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "stop",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stop_city_external_id", columnNames = {"city_id", "stop_id_ext"})
        },
        indexes = {
                @Index(name = "idx_stop_location_gist", columnList = "location"),
                @Index(name = "idx_stop_city_external", columnList = "city_id, stop_id_ext"),
                @Index(name = "idx_stop_city_name", columnList = "city_id, name")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_id_ext", nullable = false)
    private String stopIdExternal; // GTFS: stop_id

    @Column(name = "stop_code")
    private String stopCode; // GTFS: stop_code

    @Column(nullable = false)
    private String name; // GTFS: stop_name

    @Column(columnDefinition = "TEXT")
    private String description; // GTFS: stop_desc

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

//    @Column(name = "zone_id")
//    private String zoneId; // GTFS: zone_id (for fare calculation)
//
//    @Column(name = "stop_url")
//    private String url; // GTFS: stop_url
//
//    @Column(name = "location_type")
//    private Integer locationType; // GTFS: location_type (0=stop, 1=station, 2=entrance, etc.)
//
//    @Column(name = "parent_station")
//    private String parentStation; // GTFS: parent_station (ID of the parent hub)
//
//    @Column(name = "stop_timezone")
//    private String timezone; // GTFS: stop_timezone
//
//    @Column(name = "wheelchair_boarding")
//    private Integer wheelchairBoarding; // GTFS: wheelchair_boarding (0=no info, 1=yes, 2=no)
}
