package com.dnikitin.transit.infrastructure.persistence.entity;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stop")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @JdbcType(SqlTypes.GEOMETRY)
    //geo type with longitude and latitude (SRID 4326)
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326")
    private Point location;
}
