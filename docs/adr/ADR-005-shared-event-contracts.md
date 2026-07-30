# ADR-005: Shared Java Event Contracts for Initial Platform Development

## Status

Accepted

## Date

2026-07-29
---

## Context

Asterion is being designed as an **event-driven distributed platform** where services communicate asynchronously through **Apache Kafka**.

The platform requires a strategy for defining and evolving **domain event contracts** such as:

* `order.created`
* `payment.authorized`
* `inventory.reserved`
* `inventory.released`
* `payment.failed`

Two primary approaches were considered:

### Option A — Shared Java Event Contracts

Provide reusable event classes in a dedicated Maven module:

```text
libraries/events
```

All services import the same event definitions.

### Option B — Schema-Based Contracts

Define events using a serialization schema such as:

* **Apache Avro**
* **JSON Schema**
* **Protocol Buffers**

Each service generates its own classes from the shared schema and communicates through serialized contract definitions rather than shared Java code.

---

## Decision

For the **initial phase of Asterion**, use **shared Java event contracts** provided by the `libraries/events` module.

The module will contain:

* a common `DomainEvent` abstraction,
* event metadata conventions,
* immutable event payloads implemented using **Java 21 records**,
* versioned event type identifiers (for example `order.created.v1`).

Example:

```java
public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount
) implements DomainEvent {

    @Override
    public String eventType() {
        return "order.created.v1";
    }
}
```

---

## Rationale

### Advantages

#### Faster Initial Development

Shared Java contracts allow the platform to establish event-driven workflows without introducing additional schema tooling, build plugins or registry infrastructure.

#### Strong Compile-Time Safety

Producers and consumers share the same type definitions, reducing serialization mismatches during early development.

#### Simpler Refactoring

Refactoring event structures is easier while the platform is still evolving rapidly and all modules are developed within the same monorepo.

#### Better Focus for Early Milestones

The initial project milestones should prioritize:

* domain modeling,
* transactional boundaries,
* Kafka integration,
* outbox pattern implementation,
* observability,
* deployment automation.

Introducing schema infrastructure too early would increase operational complexity without providing proportional value at the current repository scale.

---

## Trade-offs

### Increased Coupling

Services become coupled to a shared Java library, which can reduce independent deployability if not managed carefully.

### Coordinated Versioning

Breaking changes to event classes require coordinated updates across dependent services.

### Less Realistic for Large Organizations

Large-scale Kafka ecosystems typically avoid sharing language-specific event classes across service boundaries and instead rely on schema contracts and compatibility validation.

---

## Consequences

The `libraries/events` module becomes a **platform-level contract library** and must follow stricter compatibility rules than ordinary internal code.

The following guidelines apply:

### Event Payloads Must Be Immutable

Use **Java records** for all event payloads.

### Event Type Names Must Be Explicitly Versioned

Examples:

* `order.created.v1`
* `payment.authorized.v1`
* `inventory.reserved.v1`

### Events Must Not Contain Framework Annotations

Event contracts should remain independent of:

* Spring Framework,
* JPA,
* Jackson-specific customization (unless absolutely required),
* Kafka-specific APIs.

### Backward Compatibility Is Preferred

New optional fields should be added in a backward-compatible manner whenever possible.

---

## Future Evolution

The current implementation uses **shared Java event contracts** through the `libraries/events` module.

As Asterion evolves toward independently deployable services and stricter event compatibility guarantees, the platform is expected to adopt a schema-based contract strategy.

### Planned Evolution

1. **Introduce Avro schemas** for externally published domain events.

   ```text
   schemas/
       order-created-v1.avsc
   ```

2. **Add Confluent Schema Registry** for producer and consumer compatibility enforcement.

3. **Generate consumer-specific event classes** from schemas during the build process.

4. **Gradually deprecate direct sharing of Java event payload classes** across independently deployable services while retaining internal compatibility adapters where necessary.

---

## Alternatives Considered

### Separate Event DTOs Per Service

**Rejected** because it would duplicate contract definitions and increase the risk of serialization drift between producers and consumers.

### JSON Schema from the Beginning

**Rejected for now** because it introduces additional tooling complexity while the platform domain model is still highly iterative.

### Avro + Schema Registry from the Beginning

**Rejected for the initial milestone** because the operational overhead is unnecessary before Kafka producers and consumers are fully established.

---

## Related ADRs

* **ADR-002:** Monorepo Strategy
* **ADR-003:** Clean Architecture
* **ADR-004:** Root Maven Parent and Aggregator Build