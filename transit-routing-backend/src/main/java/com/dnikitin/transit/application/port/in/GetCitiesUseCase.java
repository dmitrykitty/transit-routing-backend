package com.dnikitin.transit.application.port.in;

import com.dnikitin.transit.domain.model.City;

import java.util.List;

public interface GetCitiesUseCase {
    List<City> getAllCities();
}
