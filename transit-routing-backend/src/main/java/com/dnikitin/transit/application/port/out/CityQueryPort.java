package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.City;

import java.util.List;

public interface CityQueryPort {
    List<City> findAllCities();
}
