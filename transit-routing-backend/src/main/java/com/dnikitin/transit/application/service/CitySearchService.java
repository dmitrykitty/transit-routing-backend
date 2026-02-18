package com.dnikitin.transit.application.service;

import com.dnikitin.transit.application.port.in.GetCitiesUseCase;
import com.dnikitin.transit.application.port.out.CityQueryPort;
import com.dnikitin.transit.domain.model.City;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CitySearchService implements GetCitiesUseCase {

    CityQueryPort cityQueryPort;

    public List<City> getAllCities() {
        return cityQueryPort.findAllCities();
    }

}
