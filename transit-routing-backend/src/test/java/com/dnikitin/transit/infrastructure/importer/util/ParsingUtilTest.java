package com.dnikitin.transit.infrastructure.importer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingUtilTest {

    @Test
    void parsesRegularGtfsTime() {
        assertEquals(7 * 3600 + 5 * 60 + 30, ParsingUtil.parseGtfsSeconds("07:05:30"));
    }

    @Test
    void keepsOrderOfStopTimesThatCrossMidnight() {
        // GTFS writes times after midnight of the service day as 24:10:00, 25:00:00, ...
        // A trip departing 23:50 and arriving 24:10 must not arrive before it departs.
        int departure = ParsingUtil.parseGtfsSeconds("23:50:00");
        int arrival = ParsingUtil.parseGtfsSeconds("24:10:00");

        assertEquals(24 * 3600 + 10 * 60, arrival);
        assertTrue(arrival > departure);
    }

    @Test
    void acceptsSingleDigitHour() {
        assertEquals(5 * 3600, ParsingUtil.parseGtfsSeconds(" 5:00:00 "));
    }

    @Test
    void rejectsMalformedTime() {
        assertThrows(IllegalArgumentException.class, () -> ParsingUtil.parseGtfsSeconds("07:05"));
        assertThrows(IllegalArgumentException.class, () -> ParsingUtil.parseGtfsSeconds("07:65:00"));
    }
}
