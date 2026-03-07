package com.dnikitin.transit.api.rest;

import com.dnikitin.transit.api.dto.response.CityResponse;
import com.dnikitin.transit.api.mapper.CityDtoMapper;
import com.dnikitin.transit.application.port.in.GetCitiesUseCase;
import com.dnikitin.transit.domain.model.City;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
public class CityController {
    private final GetCitiesUseCase getCitiesUseCase;
    private final CityDtoMapper cityDtoMapper;

    @GetMapping
    public ResponseEntity<List<CityResponse>> getCities() {
        return ResponseEntity.ok(
                getCitiesUseCase.getAllCities().stream()
                        .map(cityDtoMapper::toCityResponse)
                        .toList()
        );
    }


}
