-- Store stop times as seconds since midnight of the service day instead of time(0).
-- GTFS times may exceed 24:00 (e.g. 24:10:00); time(0) cannot hold them and they used to wrap to 00:10.
--
-- Manual step because hibernate.ddl-auto=update neither drops the old NOT NULL columns nor can add
-- NOT NULL columns to a non-empty table. stop_time is rebuilt by the GTFS import, so it is emptied here
-- and the import is forced by clearing the stored Last-Modified headers.
--
-- Run: docker exec -i dnikitin-postgres-1 psql -U transit -d transit -v ON_ERROR_STOP=1 < db/manual-migrations/001_stop_time_seconds.sql
-- Then start the application (gtfs.initializer.enabled=true, the default) to re-import all cities.

BEGIN;

TRUNCATE stop_time;

DROP INDEX IF EXISTS idx_stop_time_departure;

ALTER TABLE stop_time
    DROP COLUMN arrival_time,
    DROP COLUMN departure_time,
    ADD COLUMN arrival_seconds integer NOT NULL,
    ADD COLUMN departure_seconds integer NOT NULL;

CREATE INDEX idx_stop_time_departure ON stop_time (departure_seconds);

UPDATE data_import_metadata SET last_modified_header = '';

COMMIT;
