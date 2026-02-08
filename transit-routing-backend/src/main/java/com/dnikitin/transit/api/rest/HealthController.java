package com.dnikitin.transit.api.rest;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of(
                        "status", "UP",
                        "service", "transit-routing-backend",
                        "timestamp", LocalDateTime.now().toString()));

    }
}
