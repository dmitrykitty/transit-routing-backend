package com.dnikitin.transit.infrastructure.adapter;

import com.dnikitin.transit.application.port.out.CityQueryPort;
import com.dnikitin.transit.domain.model.City;
import com.dnikitin.transit.infrastructure.persistence.mapper.CityEntityMapper;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CityPersistenceAdapter implements CityQueryPort {

    CityJpaRepository cityRepository;
    CityEntityMapper cityEntityMapper;


    @Override
    public List<City> findAllCities() {
        return cityRepository.findAll().stream()
                .map(cityEntityMapper::toCity)
                .toList();
    }
}
