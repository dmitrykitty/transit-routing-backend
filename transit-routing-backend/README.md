# Transit Routing & Booking Backend (In progress)

Backend project for public transport route planning and ticket booking, focusing on:

* **High-performance time-dependent routing** (graph model, RAPTOR/CSA integration)
* **Segment-based seat availability** and booking logic
* **High-throughput GTFS data ingestion**
* **Clean Architecture** and strict testability standards

## Tech Stack

* **Language/Framework**: Java 25, Spring Boot 4
* **Database**: PostgreSQL + PostGIS (Spatial indexing)
* **Deployment**: Docker Compose
* **Testing**: Testcontainers, JUnit 5
* **Utilities**: MapStruct, OpenAPI (springdoc), Univocity Parsers

## Architecture

The project follows **Clean / Hexagonal Architecture** to ensure the core routing logic remains independent of infrastructure details:

* `domain` – Core domain models (Trip, Stop, Route) and pure business rules.
* `application` – Use cases, orchestration, and ports (Input/Output interfaces).
* `infrastructure` – Persistence adapters (JDBC/JPA), GTFS file processors, and external integrations.
* `api` – REST controllers, DTOs, and request/response mapping.

---

## Data Ingestion & ETL Optimization

The system is designed to handle massive GTFS (General Transit Feed Specification) datasets for large metropolitan areas. Given the high volume of data—reaching millions of records in tables such as `stop_times` and `shape_points`—the import process has been optimized to ensure high throughput and a minimal memory footprint.

### Technical Challenges

* **Transaction Poisoning**: Standard JPA `saveAll()` methods trigger `UniqueConstraintViolation` exceptions when encountering duplicate records (e.g., shared physical stops between bus and tram bundles). In PostgreSQL, this aborts the entire transaction, making it impossible to continue without a rollback.
* **Memory Management (OOM)**: Hibernate’s First-Level Cache (Persistence Context) tracks every entity being persisted. Loading millions of `StopTime` entities into the heap leads to `OutOfMemoryError` and excessive Garbage Collection overhead.
* **Data Collisions**: GTFS feeds often overlap. The same `stop_id` or `service_id` can appear in multiple ZIP packages (e.g., `KRK_A` and `KRK_T`).

### Engineering Decisions

#### 1. JDBC Batch Processing (vs. JPA Batching)

While Spring Data JPA is used for standard domain operations, the ingestion layer was moved to **Spring JDBC Batch Updates**.

* **Stateless Persistence**: Unlike Hibernate, `JdbcTemplate` does not cache entities in memory. Data is streamed from the CSV parser and sent to the database in configurable batches (e.g., 5,000 records).
* **Performance**: This bypasses the overhead of entity lifecycle management, dirty checking, and unnecessary `SELECT` calls before `INSERT`.

#### 2. PostgreSQL `ON CONFLICT` (Upsert Logic)

To handle data collisions from multiple ZIP bundles without "poisoning" the transaction, the system utilizes native PostgreSQL upsert logic:

```sql
INSERT INTO stop_time (...) VALUES (...)
ON CONFLICT (trip_id_ext, stop_sequence, city_id) DO NOTHING;

```

* **Idempotency**: This ensures that duplicate records are silently ignored at the database level, allowing the import to proceed uninterrupted even if bundles overlap.

#### 3. Lightweight ID Mapping

To resolve foreign keys (e.g., mapping a GTFS `route_id` to the internal `BigInt` primary key), the system avoids loading full JPA entities. Instead, it uses lightweight `Map<String, Long>` structures pre-loaded via targeted SQL queries. This maintains  lookup time with a fraction of the memory cost.

#### 4. Native PostGIS Integration

Spatial data (stop locations and shape points) is generated directly within the SQL statement using:
`ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)`
This avoids the overhead of JTS (Java Topology Suite) serialization in the application layer and leverages the database's native spatial engine.

### Performance Benchmarks

The optimized ETL process demonstrates significant efficiency:

* **Throughput**: High-volume tables (`shape_points`, `stop_times`) reach ingestion speeds of **7,000 - 8,000 records/s**.
* **Scalability**: A typical dataset containing **900,000 records** is fully processed and persisted in approximately **1.5 - 2 minutes**.
* **Memory Stability**: Heap usage remains constant regardless of the total record count due to batch-clearing and streaming.

---

## Run locally
```bash
mvn clean package
mvn spring-boot:run
```