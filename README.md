# Transit Routing & Booking Platform (In Progress)

A backend-focused project for public transport journey planning and ticket booking, designed with **Clean / Hexagonal Architecture** and a roadmap for a future **frontend UI**.

## Goals (MVP vertical slice)
- Search direct trips in a time window (`/trips/search`)
- Model **segment-based seat availability** to prevent incorrect bookings on partial routes
- Implement **transaction-safe booking** with concurrency protection (optimistic locking)
- Use **PostgreSQL + PostGIS** for geospatial stop queries (e.g., “stops near me”)
- Add professional testing with **Testcontainers**

## Repository Structure
- `backend/` — Spring Boot backend (REST API, domain logic, persistence, integrations)
- `frontend/` — frontend application (planned; e.g., React)

## Architecture (Backend)
The backend follows **Hexagonal Architecture (Ports & Adapters)**:
- **Domain** (core): routing/booking rules and algorithms (no frameworks)
- **Application**: use cases + ports (`in`/`out`)
- **Infrastructure**: adapters (PostgreSQL/PostGIS, external providers, cache)
- **API**: REST controllers + DTOs

This keeps the business logic independent from frameworks and makes routing/booking logic easy to test.

## Tech Stack
**Backend:** Java 25, Spring Boot, Hibernate/JPA, PostgreSQL + PostGIS, Docker Compose, Testcontainers, MapStruct, OpenAPI  
**Frontend (later):** React (or similar), REST integration, basic UI for search/booking

## How to run
### Backend (local)
```bash
cd transit-routing-backend
mvn clean package
mvn spring-boot:run
