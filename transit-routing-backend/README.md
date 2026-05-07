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
- Docker Compose for PostgreSQL/PostGIS, Redis, and optional app container
- test profile isolated from startup GTFS seeding

Not implemented yet:

- Redis-backed distributed RAPTOR cache
- fare, accessibility, and frequency-based routing
- production deployment automation

## RAPTOR Routing

The implementation is based on:

- Daniel Delling, Thomas Pajor, Renato F. Werneck, *Round-Based Public Transit Routing*
- https://www.microsoft.com/en-us/research/publication/round-based-public-transit-routing/

Implemented behavior:

- rounds by number of boardings
- marked-stop based route collection
- earliest catchable trip scan within each route
- transfer propagation within a round
- Pareto-style journey outputs across rounds
- generated walking transfers between nearby stops
- optional date filtering before building the routing dataset

Current limitations:

- GTFS times above 24h are normalized by the importer to local time
- cache is process-local and rebuilt after app restart
- transfer generation depends on PostGIS, so H2 tests keep it disabled
- routing is earliest-arrival oriented, not full multi-criteria optimization

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
- Redis on `localhost:6379`

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
docker compose up -d postgres redis
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
- `ON CONFLICT DO NOTHING` semantics for overlapping bundles
- lightweight lookup maps for foreign-key resolution
- direct PostGIS point creation in SQL
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
