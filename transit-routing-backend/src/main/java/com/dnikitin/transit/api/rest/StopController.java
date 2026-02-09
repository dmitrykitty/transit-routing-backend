package com.dnikitin.transit.api.rest;

import com.dnikitin.transit.api.dto.response.StopResponse;
import com.dnikitin.transit.api.mapper.StopDtoMapper;
import com.dnikitin.transit.application.service.StopSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopSearchService service;
    private final StopDtoMapper mapper;

    public StopController(StopSearchService service, StopDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<StopResponse>> getAllStops(){
        return ResponseEntity.ok(
                service.findAll().stream()
                        .map(mapper::toResponse)
                        .toList());
    }


}
