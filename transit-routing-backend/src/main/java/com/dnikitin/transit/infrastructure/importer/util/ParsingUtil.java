package com.dnikitin.transit.infrastructure.importer.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ParsingUtil {
    public static Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int parseNullableIntOrDefault(String value, int defaultValue) {
        Integer result = parseNullableInt(value);
        return result != null ? result : defaultValue;
    }

    public static Double parseNullableDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static double parseNullableDoubleOrDefault(String value, double defaultValue) {
        Double result = parseNullableDouble(value);
        return result != null ? result : defaultValue;
    }

    public static boolean parseBoolean(String value) {
        return "1".equals(value);
    }

    public static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * GTFS times count from midnight of the service day and may exceed 24:00 (e.g. "25:10:00"),
     * so they are kept as seconds since that midnight instead of a time of day.
     */
    public static int parseGtfsSeconds(String value) {
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time: " + value);
        }
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("Invalid GTFS time: " + value);
        }
        return hours * 3600 + minutes * 60 + seconds;
    }
}
