## Status

Accepted

---

## Context

Asterion is being developed as a monorepo containing multiple services and reusable libraries. The build system must provide:

* centralized dependency management,
* centralized plugin management,
* consistent Java and Maven versions,
* reproducible builds,
* simple module orchestration.

---

## Decision

Use a **single root Maven project** that acts as both:

* **Parent POM** — provides shared configuration, dependency management, plugin management and build standards.
* **Aggregator POM** — coordinates all child modules through the `<modules>` section.

The root project uses:

* `packaging=pom`
* Java 21
* Spring Boot BOM
* JUnit BOM
* Testcontainers BOM
* Maven Enforcer Plugin

---

## Consequences

### Advantages

* Single source of truth for dependency versions
* Consistent plugin configuration across all modules
* Simplified upgrades
* Easier onboarding for new contributors
* Cleaner CI/CD pipelines

### Trade-offs

* All modules share the same parent lifecycle
* Root POM becomes a critical coordination point
* Large-scale refactoring must be managed carefully

---

## Alternatives Considered

### Separate Parent and Aggregator POMs

**Rejected** because the repository is currently small enough that the additional complexity provides little benefit.

### Independent Maven Projects

**Rejected** because it would duplicate dependency and plugin configuration across services and make coordinated upgrades significantly harder.
