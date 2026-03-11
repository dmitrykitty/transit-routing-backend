# Transit Routing Backend

A Java / Spring Boot backend for public transport journey planning, focused on **time-dependent routing**, **large-scale GTFS ingestion**, and **maintainable system design**.

The project explores how to build a routing engine for real transit data, where the problem is more complex than standard road navigation because journeys depend on **fixed schedules, waiting times, and transfers**.

## What this project focuses on

- computing optimal public transport journeys using a **schedule-based routing approach**
- importing and processing large **GTFS** datasets
- designing a backend that remains maintainable as routing logic evolves
- handling large data volumes with attention to **throughput, stability, and data integrity**

## Current scope

The backend currently focuses on two main areas:

### 1. Transit routing
The routing layer is designed around **time-dependent public transport search**, where results depend on departure time, trip schedules, stop sequences, and transfer opportunities.

The project is centered on a **RAPTOR-style routing approach**, chosen because it is better suited to public transport than classic shortest-path algorithms on a static graph.

### 2. GTFS data ingestion
A large part of the project is dedicated to importing GTFS feeds into a relational model that can later support routing queries.

The import pipeline is built to handle:
- **1M+ records per dataset**
- data split across multiple source archives
- repeated identifiers that cannot be treated as naive duplicates
- repeated imports without sacrificing stability

## Key engineering challenges

### Routing without a simple graph model
Public transport routing is not just "find the shortest path between two points".  
A valid journey depends on:
- timetable constraints
- departure and arrival times
- transfer windows
- the order of stops within a trip

Because of that, the project uses a **schedule-aware routing model** instead of treating the problem like standard road navigation.

### Large-scale GTFS imports
Importing GTFS data turned out to be one of the most demanding parts of the system.

The backend processes datasets with **more than one million records**, and the data may come from multiple archives with overlapping identifiers. This required careful import design to avoid accidental overwrites, preserve consistency, and keep repeated imports reliable.

To improve performance and stability, the import layer uses:
- **JDBC batching**
- controlled flushing
- staged processing of related GTFS entities
- explicit attention to write throughput and transaction boundaries

### Keeping the system evolvable
Routing logic tends to grow in complexity very quickly.  
To avoid coupling domain rules too tightly to infrastructure, the backend is structured using **Hexagonal Architecture**, so that:
- domain logic can evolve independently
- import, persistence, and API concerns stay separated
- routing algorithms can be replaced or extended more easily

## Architecture

The backend follows a **Hexagonal Architecture (Ports and Adapters)** approach.

### High-level structure
- **Domain** – routing concepts and core business rules
- **Application** – use cases and orchestration logic
- **Infrastructure** – persistence, import pipeline, and technical adapters
- **API** – controllers and transport-level concerns

This separation keeps the routing logic isolated from framework details and makes the project easier to test and extend.

## Import pipeline overview

At a high level, the GTFS import flow is:

1. read GTFS source files
2. parse records into import models
3. resolve dependencies between entities
4. persist data in a stable order
5. optimize write throughput with batching
6. keep memory and transaction behavior under control during large imports

The goal is not just to "load the data", but to do it in a way that remains reliable when datasets become large and imports are repeated regularly.

## Tech stack

- **Java**
- **Spring Boot**
- **PostgreSQL**
- **Redis**
- **JDBC batching**
- **Hexagonal Architecture**
- **GTFS**
- **RAPTOR-style routing**

## Why this project is interesting

This project sits at the intersection of:
- backend engineering
- algorithms
- data-intensive processing
- system design

It is less about building a simple CRUD application and more about designing a system that can support **non-trivial routing logic on top of large real-world transit data**.

## Project status

This project is **in progress**.

The current focus is on:
- strengthening the GTFS import pipeline
- improving routing logic and performance
- refining the backend architecture as the system grows

## Possible next steps

Planned or potential future work includes:
- richer routing queries
- better transfer modeling
- caching and performance improvements
- broader API coverage
- more testing around import and routing behavior

## Running the project


```bash
cd transit-routing-backend
mvn clean package
mvn spring-boot:run
```

## Repository structure

- transit-routing-backend/ – backend application
- transit-routing-frontend/ – frontend area / experiments
