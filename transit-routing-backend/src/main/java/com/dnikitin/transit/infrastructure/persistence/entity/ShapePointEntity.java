package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(
        name = "shape_point",
        indexes = {
                @Index(name = "idx_shape_id_sequence", columnList = "shape_id_ext, point_sequence")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShapePointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shape_id_ext", nullable = false)
    private String shapeIdExternal; // GTFS: shape_id (np. "shape_6254")

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location; // Mapping GTFS: shape_pt_lat and shape_pt_lon

    @Column(name = "point_sequence", nullable = false)
    private Integer sequence; // GTFS: shape_pt_sequence

    @Column(name = "shape_dist_traveled")
    private Double distTraveled; // GTFS: shape_dist_traveled
}