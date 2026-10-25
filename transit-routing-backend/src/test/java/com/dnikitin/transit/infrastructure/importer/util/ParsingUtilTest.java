package com.dnikitin.transit.infrastructure.importer.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingUtilTest {

    @Test
    void parsesRegularGtfsTime() {
        assertEquals(LocalTime.of(7, 5, 30), ParsingUtil.parseGtfsTime("07:05:30"));
    }

    @Test
    @Disabled("Known bug: parseGtfsTime takes hour % 24 and stop_time stores LocalTime, so times after 24:00 wrap to the early morning")
    void keepsOrderOfStopTimesThatCrossMidnight() {
        // GTFS writes times after midnight of the service day as 24:10:00, 25:00:00, ...
        // A trip departing 23:50 and arriving 24:10 must not arrive before it departs.
        LocalTime departure = ParsingUtil.parseGtfsTime("23:50:00");
        LocalTime arrival = ParsingUtil.parseGtfsTime("24:10:00");

        assertTrue(arrival.isAfter(departure), "24:10:00 parsed as " + arrival + ", before 23:50:00");
    }
}
