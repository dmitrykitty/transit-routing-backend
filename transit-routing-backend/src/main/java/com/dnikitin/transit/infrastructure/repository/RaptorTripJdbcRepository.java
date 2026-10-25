package com.dnikitin.transit.infrastructure.repository;

import com.dnikitin.transit.application.port.out.raptor.RaptorStopTimeData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.*;

/**
 * Loads trips with their stop times for the RAPTOR data set as plain rows (no JPA entities), limited to
 * the given service calendars. Rows are streamed with a fetch size, so the driver never holds the whole
 * result; callers should run inside a read-only transaction for PostgreSQL to use a cursor.
 */
@Repository
public class RaptorTripJdbcRepository {

    private static final int FETCH_SIZE = 10_000;

    private static final String TRIPS_SQL = """
            SELECT t.id, t.route_id, t.direction_id, t.trip_headsign
            FROM trip t
            WHERE t.city_id = :cityId %s
            ORDER BY t.id
            """;

    private static final String STOP_TIMES_SQL = """
            SELECT st.trip_id, st.stop_id, st.arrival_seconds, st.departure_seconds, st.stop_sequence
            FROM stop_time st
            JOIN trip t ON t.id = st.trip_id
            WHERE t.city_id = :cityId %s
            ORDER BY st.trip_id, st.stop_sequence
            """;

    private static final String STOP_NAMES_SQL = "SELECT id, name FROM stop WHERE city_id = :cityId";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RaptorTripJdbcRepository(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setFetchSize(FETCH_SIZE);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(template);
    }

    /**
     * @param calendarIds internal service_calendar ids whose trips to load, or {@code null} for all trips of the city
     */
    public List<RaptorTripData> findTripsWithStopTimes(short cityId, Collection<Long> calendarIds) {
        if (calendarIds != null && calendarIds.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource("cityId", cityId);
        String calendarFilter = "";
        if (calendarIds != null) {
            parameters.addValue("calendarIds", calendarIds);
            calendarFilter = "AND t.calendar_id IN (:calendarIds)";
        }

        Map<Long, String> stopNames = new HashMap<>();
        jdbcTemplate.query(STOP_NAMES_SQL, parameters, rs -> {
            stopNames.put(rs.getLong(1), rs.getString(2));
        });

        Map<Long, List<RaptorStopTimeData>> stopTimesByTrip = new HashMap<>();
        jdbcTemplate.query(STOP_TIMES_SQL.formatted(calendarFilter), parameters, rs -> {
            long stopId = rs.getLong(2);
            stopTimesByTrip.computeIfAbsent(rs.getLong(1), ignored -> new ArrayList<>())
                    .add(new RaptorStopTimeData(stopId, stopNames.get(stopId), rs.getInt(3), rs.getInt(4), rs.getInt(5)));
        });

        List<RaptorTripData> trips = new ArrayList<>();
        jdbcTemplate.query(TRIPS_SQL.formatted(calendarFilter), parameters, rs -> {
            long tripId = rs.getLong(1);
            trips.add(new RaptorTripData(
                    tripId,
                    rs.getLong(2),
                    rs.getObject(3, Integer.class),
                    rs.getString(4),
                    List.copyOf(stopTimesByTrip.getOrDefault(tripId, List.of()))
            ));
        });

        return trips;
    }
}
