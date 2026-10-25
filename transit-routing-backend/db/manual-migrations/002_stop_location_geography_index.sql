-- Spatial index for the footpath query in StopJpaRepository.findTransferCandidatesByCity, which joins
-- stops with ST_DWithin(location::geography, location::geography, radius). Without it PostgreSQL
-- compares every pair of stops of the city in a nested loop.
--
-- An expression index cannot be declared with @Index on StopEntity, so it is created here; Hibernate's
-- ddl-auto=update leaves it in place.
--
-- Run: docker exec -i dnikitin-postgres-1 psql -U transit -d transit -v ON_ERROR_STOP=1 < db/manual-migrations/002_stop_location_geography_index.sql

CREATE INDEX IF NOT EXISTS idx_stop_location_geography ON stop USING gist ((location::geography));

ANALYZE stop;
