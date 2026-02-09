package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id",  nullable = false)
    private ProviderEntity provider;

    @Builder.Default
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @OrderBy("stopSequence ASC")
    private List<RouteStopEntity> stops = new ArrayList<>();

    public void addRouteStop(RouteStopEntity stop) {
        stops.add(stop);
        stop.setRoute(this);
    }
}
