# ADR-006: Testcontainers-Based Integration Testing

## Status

Accepted

## Date

2026-07-30

---

## Context

Asterion services depend on infrastructure components such as PostgreSQL, Apache Kafka, and Redis. Traditional approaches using in-memory substitutes (for example H2) do not accurately reproduce production behavior and often hide integration issues related to SQL dialects, transactions, Kafka serialization and container networking.

## Decision

Use **Testcontainers** as the standard approach for integration testing across all platform services and reusable testing libraries.

The testing foundation will provide reusable container support for:

* PostgreSQL
* Apache Kafka
* Redis

All service-level integration tests should execute against real containerized dependencies rather than embedded or mocked infrastructure whenever persistence, messaging or distributed coordination behavior is being validated.

## Consequences

### Advantages

* Production-like infrastructure behavior
* Reproducible local and CI environments
* Better confidence in transactional and messaging workflows
* Reduced environment drift between developer machines and GitHub Actions

### Trade-offs

* Slower test startup compared to pure unit tests
* Docker is required for integration test execution
* Additional container lifecycle management is needed

## Alternatives Considered

### H2 In-Memory Database

Rejected because PostgreSQL-specific behavior is important for transactional integrity, indexing, JSONB support and migration validation.

### Shared External Test Infrastructure

Rejected because it introduces flakiness, shared-state problems and onboarding complexity.

## Related ADRs

* ADR-004: Root Maven Parent and Aggregator Build
* ADR-005: Shared Java Event Contracts for Initial Platform Development
