# Transit Routing Backend

Java/Spring backend for GTFS ingestion, transit catalog APIs, and RAPTOR-based public transport journey planning.

The project is finished as a routing MVP. Booking, seat inventory, and provider aggregation from the original sprint idea are intentionally kept as future modules.

## Architecture

The codebase follows a hexagonal structure:

```text
api
  REST controllers, DTO responses, exception handling
application
  use cases, orchestration, cache boundary, read-model ports
domain
  pure routing model and RAPTOR algorithm
infrastructure
  JPA/PostGIS persistence, GTFS import, scheduler, configuration
```

Key rule: RAPTOR routing logic stays in `domain.service.RaptorRouter`; data loading and caching stay in `application`; JPA entities stay inside `infrastructure`.

## Current Status

Implemented:

- GTFS import pipeline with JDBC-heavy bulk loading for large feeds
- city, stop, and route catalog APIs
- RAPTOR dataset builder from imported trips/stops/stop_times
- service-date filtering using GTFS `calendar.txt` and `calendar_dates.txt`
- in-memory per-city/per-date RAPTOR dataset cache
- cache invalidation after GTFS re-import
- transfer generation from stop proximity using PostGIS
- RAPTOR journey endpoint with optional `serviceDate`
- OpenAPI/Swagger UI
- Docker Compose for PostgreSQL/PostGIS and optional app container
- test profile isolated from startup GTFS seeding

Not implemented yet:

- fare, accessibility, and frequency-based routing
- production deployment automation

## RAPTOR Routing

The implementation is based on:

- Daniel Delling, Thomas Pajor, Renato F. Werneck, *Round-Based Public Transit Routing*
- https://www.microsoft.com/en-us/research/publication/round-based-public-transit-routing/

Implemented behavior:

- round k holds the best arrivals with exactly k trips; footpaths from the source are relaxed in round 0
- marked-stop based route collection, each route scanned from its earliest marked stop
- earliest catchable trip found by binary search; trips that overtake each other on the same stop pattern are
  split into separate RAPTOR routes (`FifoTripPartitioner`), so departures of a route are sorted at every stop
- footpath propagation after each round (chained walks allowed), target pruning
- Pareto set over (arrival time, number of trips): one journey per round that improves the arrival at the target
- generated walking transfers between stops within 150 m (PostGIS, walking speed 1.4 m/s)
- service-date filtering with `calendar.txt` / `calendar_dates.txt`; a data set for date D also contains the
  previous service day's trips that run after midnight (shifted by -24 h)
- GTFS times past 24:00 are stored as seconds since midnight of the service day

Current limitations:

- times past midnight are shown as wrapped local times (`00:10`) in the API, without a day offset
- no minimum change time at the same stop, and walking chains are not length-limited beyond the 300 s per footpath
- the data set cache is process-local and keeps one entry per requested date; it is rebuilt after a restart
- without `serviceDate` all service days are mixed into one data set, so journeys can use trips that do not run
  on the travel date; pass `serviceDate` for correct results
- transfer generation depends on PostGIS, so H2 tests keep it disabled
- criteria are arrival time and number of trips only (no walking time, fares or accessibility)

Correctness is checked by `RaptorRouterReferenceTest`: 500 seeded random networks (including trips that overtake
each other) with 20 queries each are compared with a brute-force fixed-point search over all trips and footpaths
(`BruteForceEarliestArrival`), and every returned journey is validated leg by leg against the timetable
(`JourneyValidator`). `RaptorRouterScenarioTest` covers hand-made cases.

## API

### Health

```http
GET /api/v1/health
```

### Cities

```http
GET /api/v1/cities
```

### Stops

```http
GET /api/v1/stops
```

Legacy path still works:

```http
GET /api/stops
```

### Routes

```http
GET /api/v1/routes/{cityId}
GET /api/v1/routes/{cityId}?type=TRAM
GET /api/v1/routes/{cityId}/{number}?type=TRAM
```

Legacy `/api/routes/...` paths still work.

### RAPTOR Dataset Inspection

```http
GET /api/v1/raptor/cities/{cityId}/dataset?routeLimit=10&stopLimit=10
```

Response includes stop count, route count, generated transfer count, and compact route/stop summaries.

### RAPTOR Journey Search

```http
GET /api/v1/raptor/cities/{cityId}/journeys?sourceStopId=101&targetStopId=233&departureTime=08:15
```

With date-aware GTFS calendar filtering:

```http
GET /api/v1/raptor/cities/{cityId}/journeys?sourceStopId=101&targetStopId=233&departureTime=08:15&serviceDate=2026-04-28
```

Required params:

- `sourceStopId`
- `targetStopId`
- `departureTime`, ISO local time such as `08:15` or `08:15:00`

Optional params:

- `serviceDate`, ISO local date such as `2026-04-28`

### OpenAPI

After starting the app:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration

Default runtime dependencies:

- PostgreSQL/PostGIS on `localhost:5434`

Important settings:

```yaml
raptor:
  transfer:
    enabled: true
    radius-meters: 150.0
    walking-speed-meters-per-second: 1.4
    max-duration-seconds: 300
```

`spring.jpa.open-in-view=false` is set so database access stays inside service/repository boundaries.

## Running Locally

Start infrastructure:

```bash
docker compose up -d postgres
```

Run tests:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Run the application locally:

```bash
./mvnw spring-boot:run
```

Run with Docker Compose app container:

```bash
docker compose --profile app up --build
```

## Database Migrations

The schema is created by Hibernate (`ddl-auto: update`). Changes it cannot apply are in `db/manual-migrations`:

- `001_stop_time_seconds.sql`: only for databases created before stop times were stored as seconds. It empties
  `stop_time`, replaces the `time` columns with `arrival_seconds`/`departure_seconds` and forces a re-import on
  the next start. Do not run it on a database created by the current code.
- `002_stop_location_geography_index.sql`: GiST index used by the footpath query. Run it once after the first
  start (the tables must exist); it is idempotent and `bench/run-benchmarks.sh` applies it as well.

```bash
docker compose exec -T postgres psql -U transit -d transit < db/manual-migrations/002_stop_location_geography_index.sql
```

## Performance

### Reproducing

```bash
bench/run-benchmarks.sh
```

The script starts PostgreSQL, builds the project, runs every benchmark below and prints a Markdown report (also
saved as `target/bench/run-<timestamp>/report.md` next to the raw JMH, k6 and harness outputs). It needs Docker,
JDK 25, curl and [k6](https://k6.io) (on `PATH` or `K6=/path/to/k6`; the HTTP part is skipped without it). With
three imports it took about 50 minutes on the machine below; `SKIP_IMPORT=1` runs only the query benchmarks
against an already imported database. Other overrides (service date, number of queries, seed, virtual users,
`QUICK=1` for a smoke test) are listed at the top of the script. The default service date must lie inside the
validity of the downloaded feeds.

### Method

- **Data**: the three Krakow GTFS feeds (`KRK_A` buses, `KRK_T` trams, `KRK_M`), downloaded once and served from
  a local HTTP server during the import runs.
- **Import** (`GtfsImportBenchmark`): `GtfsImportService.performFullCityUpdate`, the code path of the scheduler.
  It clears the city, imports all files of the three archives with JDBC batches of 5,000 rows
  (`reWriteBatchedInserts=true`, `ON CONFLICT DO NOTHING`) in a single transaction and materializes `route_stop`.
  Unzipping is included, the download is not. Three consecutive runs in one JVM.
- **Queries**: 1,000 origin-destination pairs, source and target uniformly random among the stops served on the
  date, departure uniformly random 06:00-21:59 (whole minutes), seed 42, service date Tuesday 2026-10-27.
- **Algorithm only** (`RaptorRouterBenchmark`): JMH 1.37 `SampleTime`, one thread, 2 forks of 5 x 10 s warm-up
  and 5 x 20 s measurement, on a serialized snapshot of the same data set (no database involved). Each invocation
  runs the next query of the set.
- **HTTP**: the application jar (`-Xms8g -Xmx8g`), the data set cache filled by one request, then k6 in a closed
  model: 60 s warm-up with 8 virtual users, then 60 s each with 1 and 8 virtual users replaying the same queries
  against `GET /api/v1/raptor/cities/1/journeys`.
- **Cold data set build** (`RaptorCacheBenchmark`): building the RAPTOR data set for a date that is not cached,
  with a breakdown of the database loads.

### Results

Run of 2026-10-25 (`run-20261025-224545`, application sources as committed in `7031c07`) on an Intel Core
i7-10750H (6 cores / 12 threads), 31.8 GB RAM,
Windows 11 Pro, Oracle JDK 25 (25+37, G1), PostgreSQL 18 / PostGIS 3.6 in Docker on the same machine.

| Network (Krakow, feeds published 22-23 Oct 2026) | |
|---|---|
| Whole feed | 4,115 stops, 221 routes, 124,753 trips, 2,730,189 stop times |
| RAPTOR data set for 2026-10-27 | 4,054 served stops, 595 route patterns, 16,734 trips, 379,900 stop events, 9,886 footpaths |
| Feed versions | `KRK_A` 20261021_20261024, `KRK_T` 20261023, `KRK_M` 1693_1694 |

| GTFS import (full city, 3 runs) | |
|---|---|
| Runs | 623.9 s, 643.0 s, 654.9 s (median 643.0 s) |
| Rows after import | 4,686,608 (2,730,189 stop times, 1,821,706 shape points, 124,753 trips, ...) |

| Query latency | p50 | p95 | p99 | Throughput | Samples |
|---|---|---|---|---|---|
| Algorithm only (JMH, 1 thread) | 4.678 ms | 9.110 ms | 13.042 ms | - | 42,352 |
| HTTP, 1 virtual user (k6, 60 s) | 7.01 ms | 13.39 ms | 18.36 ms | 137.8 req/s | 8,270 requests, 0 failed |
| HTTP, 8 virtual users (k6, 60 s) | 16.33 ms | 29.95 ms | 39.41 ms | 484.2 req/s | 29,062 requests, 0 failed |

| Cold RAPTOR data set build | |
|---|---|
| Fresh JVM, 2026-10-27 | 2,600 ms |
| Same JVM, 2026-10-27 then 2026-10-28 | 2,894 ms, 1,889 ms |
| Breakdown: trips and stop times / footpaths / stops | 854 ms / 160 ms / 91 ms |
| First HTTP journey request after application start | 3.48 s |
| Heap after GC: empty cache / one date / two dates | 38 MB / 56 MB / 62 MB |
| Cache hit (`buildForCity` on a cached date) | 0.287 µs per call |

### Optimizations and fixes, measured before and after

Each change was measured with the same harness and query set immediately before and after it (earlier runs on the
same machine; absolute numbers differ slightly between runs).

| Commit | Change | Before | After |
|---|---|---|---|
| `714579f` | GiST index on `stop.location::geography` for the footpath query | footpath query 29,935 ms; cold build 90.6 s / 84.8 s | 160 ms; 57.5 s / 50.1 s |
| `490a16d` | binary search for the earliest catchable trip (JFR: linear scan was 26.8% of samples) | JMH p50 / p95 / p99 5.603 / 11.518 / 18.272 ms | 4.932 / 9.552 / 13.585 ms |
| `4772dc7` | path steps allocated only for improving arrivals | 6,880,273 B allocated per query | 5,624,088 B (latency change within noise) |
| `1b42649` | trips and stop times of the active calendars loaded with JDBC instead of all stop times as JPA entities | cold build 53.3 s / 48.3 s; trip load 23,796 ms | 2.7 s / 1.7 s; 604 ms |

Correctness fixes that changed what is measured: `c773b68` (footpaths from the source in round 0; before it, 76 of
8,401 random reference queries returned a dominated journey) and `f99a7c5` (stop times past 24:00 wrapped to the
early morning; on the pre-fix data, 985 of the 998 benchmark queries that found a journey contained a ride arriving
before it departed). Latency numbers measured before these fixes are not comparable and are not reported.

### Caveats

- One laptop, one city, one service date (a Tuesday; weekend data sets are smaller). The laptop has turbo boost and
  ordinary background load, and p99 moved by several milliseconds between otherwise identical runs; p50 and p95
  are the more stable numbers.
- PostgreSQL runs in Docker and k6 on the same machine as the application, so with 8 virtual users the load
  generator and the server share the same 6 cores. The HTTP numbers include Spring MVC, JSON serialization and
  journey reconstruction; the JMH numbers are the routing algorithm only.
- Origin-destination pairs are uniformly random over served stops, not weighted by real demand; 987 of the 1,000
  queries have a journey.
- The import time covers all tables of all three feeds, including 1.8 million shape points that routing does not
  use, inside one transaction with all indexes maintained. The earlier JPA-based importer was not benchmarked, so
  no before/after number is claimed for the move to JDBC batching.
- The data set cache lives in the JVM heap, one entry per requested date; the cold build is paid again after a
  restart or a GTFS re-import.

## Test Profile

Tests use:

- H2 in-memory database
- `gtfs.initializer.enabled=false`
- generated transfers disabled
- SpringDoc endpoints disabled

This keeps automated tests fast and independent from live GTFS downloads or PostGIS.

## GTFS Import Notes

The import path is optimized around large feeds:

- JDBC batch inserts instead of entity-heavy JPA persistence for hot paths
- `ON CONFLICT DO NOTHING` semantics for overlapping bundles; duplicates are skipped silently, so bundles must not
  reuse ids for different things (the October 2026 Krakow feeds share no stop, route, trip or service ids)
- lightweight lookup maps for foreign-key resolution
- direct PostGIS point creation in SQL
- stop times stored as seconds since midnight of the service day, so values past 24:00 keep their order
- scheduler checks `Last-Modified` headers and triggers full city refreshes

After a city refresh, the RAPTOR dataset cache for that city is invalidated.

## Future Booking Engine

The original project plan included booking and seat inventory. That should be added as a separate vertical slice after the routing MVP:

- reservation and seat availability domain models
- optimistic locking for double-booking protection
- booking REST endpoint
- concurrency tests
- provider aggregation/mock provider adapter

This keeps the current backend focused and demoable as a transit routing service while leaving a clear path toward the broader booking product.
