# Transit Routing & Booking Backend (In progress)

Backend project for public transport route planning and ticket booking, focusing on:
- time-dependent routing (graph model, transfers)
- segment-based seat availability
- clean architecture and testability

## Tech Stack
- Java 25, Spring Boot
- PostgreSQL + PostGIS
- Docker Compose
- Testcontainers
- MapStruct, OpenAPI (springdoc)

## Architecture
Clean / Hexagonal Architecture:
- `domain` – core domain model and business rules
- `application` – use cases and ports (in/out)
- `infrastructure` – persistence, external providers, integrations
- `api` – REST controllers and DTOs

## Current Status
- Project skeleton and API bootstrap  
- Next: PostgreSQL/PostGIS + Docker Compose, base domain model, search endpoint

## Run locally
```bash
mvn clean package
mvn spring-boot:run
```