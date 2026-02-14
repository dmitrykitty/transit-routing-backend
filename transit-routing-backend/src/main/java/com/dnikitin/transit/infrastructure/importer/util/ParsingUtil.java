package com.dnikitin.transit.infrastructure.importer.util;

import lombok.experimental.UtilityClass;

import java.time.LocalTime;

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

    public static LocalTime parseGtfsTime(String value) {
        String[] parts = value.trim().split(":");
        int hour = Integer.parseInt(parts[0]) % 24; // Obsługa GTFS 24h+
        return LocalTime.of(hour, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
