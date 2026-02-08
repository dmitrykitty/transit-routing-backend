package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provider")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString(exclude = "routes")
@EqualsAndHashCode(exclude = "routes")
public class ProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteEntity> routes = new ArrayList<>();


    public void addRoute(RouteEntity route) {
        routes.add(route);
        route.setProvider(this);
    }
}
