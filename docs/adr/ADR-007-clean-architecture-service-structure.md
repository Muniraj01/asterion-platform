# ADR-007: Clean Architecture Service Structure

## Status

Accepted

## Date

2026-07-31

---

## Context

Asterion services are expected to grow in business complexity and operational scope. Traditional layered structures such as:

```text
controller/
service/
repository/
```

often lead to:

* framework annotations leaking into the domain model,
* tightly coupled business logic,
* difficult unit testing,
* unclear separation between application workflows and infrastructure concerns.

Because Asterion is designed as a **distributed platform composed of independently deployable services**, architectural boundaries and dependency direction must remain explicit from the beginning.

---

## Decision

Adopt a **Clean Architecture / Ports and Adapters** structure for all Spring Boot services.

Each service will follow this package organization:

```text
domain/           - business entities, value objects, domain rules
application/      - use cases, ports, orchestration logic
infrastructure/   - persistence, messaging, security, external integrations
interfaces/       - REST controllers, API DTOs, transport adapters
```

Dependency direction must always point **inward**:

```text
interfaces -> application -> domain
infrastructure -> application -> domain
```

The **domain layer must not depend on Spring Framework, JPA, Hibernate, Kafka, or other infrastructure technologies**.

---

## Consequences

### Advantages

#### Clear Separation of Concerns

Business rules remain isolated from transport, persistence, and framework concerns.

#### Improved Testability

Domain and application layers can be tested without starting Spring Boot or external infrastructure.

#### Reduced Framework Coupling

Changing REST adapters, persistence technology, or messaging infrastructure requires fewer changes to core business logic.

#### Better Long-Term Maintainability

Use cases become explicit application entry points rather than being hidden inside large service classes.

#### Easier Team Ownership

Different engineers can work on API adapters, persistence adapters, and business rules with reduced merge conflicts and clearer responsibilities.

### Trade-offs

#### More Initial Boilerplate

Additional packages, interfaces, and mapping code are required compared to a simple layered architecture.

#### Higher Learning Curve

Developers unfamiliar with Clean Architecture may initially find the separation between application and infrastructure layers less intuitive.

#### Potential Over-Engineering for Small Services

Simple CRUD-only services may require slightly more structure than strictly necessary, although the platform prioritizes **consistency across services** over minimizing initial boilerplate.

---

## Alternatives Considered

### Traditional Layered Architecture

```text
controller/
service/
repository/
entity/
```

**Rejected** because business logic tends to accumulate inside service classes, making dependency boundaries difficult to enforce and increasing coupling to Spring and JPA.

### Feature-by-Package Without Architectural Boundaries

**Rejected** because it improves discoverability but does not prevent infrastructure concerns from leaking into business logic.

### Separate Maven Modules per Architectural Layer

**Deferred** because it introduces significant build and dependency-management complexity at the current stage of the platform. Package-level separation inside each service provides a better balance between architectural clarity and development velocity.

---

## Related ADRs

* **ADR-003:** Clean Architecture
* **ADR-004:** Root Maven Parent and Aggregator Build
* **ADR-006:** Testcontainers-Based Integration Testing
