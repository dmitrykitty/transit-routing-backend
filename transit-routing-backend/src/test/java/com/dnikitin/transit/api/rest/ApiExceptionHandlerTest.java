package com.dnikitin.transit.api.rest;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsIllegalArgumentToConsistentBadRequestBody() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Unknown stop id: 10")
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("Unknown stop id: 10", response.getBody().get("message"));
    }

    @Test
    void mapsValidationFailureToBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(
                new ConstraintViolationException("routeLimit must be less than or equal to 200", null)
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("routeLimit must be less than or equal to 200", response.getBody().get("message"));
    }
}
