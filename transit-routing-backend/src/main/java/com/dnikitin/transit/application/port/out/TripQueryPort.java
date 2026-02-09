package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.domain.model.Trip;

import java.time.OffsetDateTime;
import java.util.List;

public interface TripQueryPort {
    List<Trip> findByDepartureWindow(OffsetDateTime from, OffsetDateTime to);
}
