# ASTERION PLATFORM
# Engineering Project Handover & AI Development Context

**Document purpose:** Enable any AI coding assistant or engineer to continue development without depending on previous ChatGPT conversations.

**Last updated:** 7 November 2026  
**Repository:** `Muniraj01/asterion-platform`  
**Local repository:** `C:\opt\data\asterion-platform`  
**Current working branch:** `feature/manual-risk-review-workflow`  
**Project stage:** Payment Service — Manual Risk Review Workflow implementation  
**Document status:** Initial comprehensive handover; verify against the current Git working tree before making changes.

---

# 1. Executive summary

Asterion Platform is an event-driven commerce and payment backend built to demonstrate production-oriented backend engineering and distributed systems design.

The project is intended to demonstrate the engineering capabilities expected of a Senior/Staff Backend Engineer:

- Designing maintainable distributed service architectures.
- Applying domain-driven design and clean architecture.
- Building event-driven systems.
- Designing reliable transactional outbox patterns.
- Handling duplicate events and idempotency.
- Managing concurrent state transitions.
- Building retry and recovery mechanisms.
- Designing secure service-to-service and user-facing APIs.
- Writing meaningful unit, integration and failure-path tests.
- Making defensible engineering trade-offs.

This is an engineering portfolio project, not a toy CRUD application.

The system models a commerce/payment workflow:

```text
Customer
   |
   v
API Gateway
   |
   +--------------------+
   |                    |
   v                    v
Auth Service       Merchant Service
                        |
                        v
                   Order Service
                        |
                  OrderCreatedEvent
                        |
                        v
                  Payment Service
                        |
               Automated Risk Assessment
                        |
             +----------+----------+
             |          |          |
             v          v          v
          APPROVE     REJECT      REVIEW
             |          |          |
             v          v          v
         Processor    Failed     Manual Review
             |        Outcome      Workflow
             |
       +-----+------+
       |            |
       v            v
    SUCCESS       FAILURE
       |
       v
PaymentSucceededEvent
       |
       v
   Ledger Service
       |
       v
  Double-entry Ledger
```

The current implementation includes the main merchant, order, payment and ledger foundations.

The immediate objective is to finish the **Manual Risk Review Workflow** correctly, including authorization, auditability, transactional persistence, concurrency protection and safe recovery.

---

# 2. Product and engineering objectives

## 2.1 Functional objective

Build a realistic commerce/payment platform with independent services and asynchronous workflows.

The target functional journey is:

1. A customer authenticates.
2. A merchant is registered and validated.
3. A customer places an order.
4. The order service persists the order.
5. An order-created event is published.
6. Payment service receives the event.
7. Payment service performs risk assessment.
8. Approved payments are processed.
9. Rejected payments produce failure outcomes.
10. Payments requiring review enter a human review queue.
11. Successful payment events are consumed by the ledger.
12. Ledger entries are recorded idempotently.

Additional future functionality includes fraud/risk, bank integration and notifications.

## 2.2 Engineering objective

The platform must demonstrate:

- Strong domain boundaries.
- Explicit state machines.
- Consistent database transactions.
- Reliable event delivery.
- Duplicate event handling.
- Concurrent request handling.
- Retry with backoff.
- Lease-based recovery.
- Auditability.
- Security at service boundaries.
- Integration testing against real infrastructure where appropriate.

## 2.3 What the platform does not currently claim

Do not represent the following as implemented:

- Real bank/acquirer integration.
- Real money movement.
- Production payment processor connectivity.
- Exactly-once message delivery or processing.
- Refund workflows.
- Settlement and reconciliation.
- Ledger fees.
- A fully operational fraud intelligence platform.
- Production deployment to AWS/Kubernetes.

A stub payment processor currently represents the external payment-processing boundary.

---

# 3. Technology stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Application framework | Spring Boot 3.5.x |
| Build | Maven |
| Database | PostgreSQL 16 |
| Persistence | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Messaging | Apache Kafka |
| Caching | Redis |
| Containers | Docker |
| Orchestration target | Kubernetes |
| Cloud target | AWS |
| Unit testing | JUnit 5, Mockito |
| Integration testing | Testcontainers |

The root Maven project is a multi-module project.

Do not introduce technologies that are not already part of the architecture without explaining the reason and obtaining agreement.

RabbitMQ is not an established project dependency.

---

# 4. Repository and module structure

Repository:

`C:\opt\data\asterion-platform`

GitHub:

`https://github.com/Muniraj01/asterion-platform`

The main service boundaries include:

| Module | Responsibility |
|---|---|
| API Gateway | External API routing and gateway security |
| Auth Service | Registration, authentication, JWT and roles |
| Merchant Service | Merchant domain and lifecycle |
| Order Service | Order creation and lifecycle |
| Payment Service | Payment processing and risk |
| Ledger Service | Financial ledger posting |
| Shared event contracts | Cross-service event definitions |

The exact Maven module names and source paths must be verified from the root `pom.xml` and current repository structure before implementation.

## Architecture conventions

The intended architecture follows a clean/hexagonal approach:

- Domain models and rules belong in domain packages.
- Application services coordinate use cases.
- Ports define repository and external-system contracts.
- Infrastructure adapters implement ports.
- API controllers handle transport concerns.
- DTOs should not leak persistence entities.
- Event contracts should remain independent of implementation details.

Do not introduce framework dependencies into domain classes merely for convenience.

---

# 5. Current Git and development state

## Current branch

`feature/manual-risk-review-workflow`

The README documentation branch has already been merged.

The Manual Risk Review Workflow is being developed separately.

Do not assume the current branch is clean. Always inspect:

```powershell
git status --short
git branch --show-current
git log -5 --oneline
```

Before changing code, inspect the current working tree and preserve uncommitted user work.

## Working agreements

The developer:

- Works on Windows using PowerShell and IntelliJ.
- Manually edits source files.
- Does not want unsolicited scripts that write or overwrite project files.
- Wants exact file paths and complete source contents.
- Wants complete implementation packages rather than repeated partial iterations.
- Wants all necessary files requested in one consolidated inspection command.
- Requires diff review before commits.
- Does not want commits made without explicit approval.

These are important requirements for any AI assistant continuing the project.

---

# 6. Implemented functionality

The following represents the last known implementation status from development sessions. Verify actual source code and tests before changing anything.

## 6.1 Authentication

Existing capabilities:

- User registration.
- User authentication.
- JWT token generation.
- USER and ADMIN roles.
- Bootstrap administrator initialization.
- Normal registrations default to USER.

Role definition:

```java
public enum Role {
    USER,
    ADMIN
}
```

The JWT contains:

- Subject.
- Email.
- Roles.

The subject is the user UUID.

The gateway uses JWT role claims and the `ROLE_` authority prefix.

## 6.2 API Gateway

Existing configuration includes routes for:

- Authentication endpoints.
- User endpoints.
- Admin endpoints.
- Merchant service.
- Order service.

The gateway currently has:

- Reactive security.
- JWT resource-server validation.
- Role-based authorization.

Important: the last inspected Gateway configuration did not contain a Payment Service route.

A payment route must be added when the payment review API is implemented.

## 6.3 Merchant Service

Existing implementation includes:

- Merchant domain model.
- Merchant lifecycle.
- Merchant validation.
- Persistence.
- Database migrations.
- Unit and integration tests.

Order creation validates that the merchant is active.

## 6.4 Order Service

Existing implementation includes:

- Order creation.
- Order persistence.
- Merchant validation.
- Order lifecycle.
- Order-created event publication through the outbox.
- Order status management.

Existing order statuses include:

- CREATED
- CANCELLED
- COMPLETED

The order service has comprehensive unit and integration tests.

## 6.5 Payment Service

Existing implementation includes:

- Payment initiation.
- Duplicate initiation protection.
- Payment domain model.
- Payment repository port.
- JPA persistence.
- Kafka event consumption.
- Automated risk assessment.
- Stub payment processor.
- Retry scheduling.
- Processing leases.
- Recovery scheduler.
- Transactional outbox.
- Payment success/failure events.

### Existing payment statuses

```java
public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    RETRYABLE,
    SUCCEEDED,
    FAILED,
    RISK_REJECTED,
    RISK_REVIEW
}
```

### Existing risk decisions

```java
public enum RiskDecision {
    APPROVE,
    REJECT,
    REVIEW
}
```

### Automated risk workflow

- APPROVE: payment proceeds to the processor.
- REJECT: payment transitions to `RISK_REJECTED` and a failure event is published.
- REVIEW: payment transitions to `RISK_REVIEW` and waits for manual intervention.

### Payment processor boundary

The processor port accepts:

```java
PaymentProcessingResult process(
    Payment payment,
    String idempotencyKey
);
```

The current idempotency key is:

```java
payment.paymentId().toString()
```

The processor is a stub.

Do not claim that the stub proves external processor idempotency or that the platform guarantees exactly-once payment execution.

### Payment outbox

The payment outbox supports:

- NEW
- PROCESSING
- PUBLISHED

It uses claimed records and publication timestamps.

The outbox publisher is separate from payment processing.

### Payment recovery

The scheduler:

1. Handles expired processing attempts that have exceeded the retry limit.
2. Finds eligible payment IDs.
3. Invokes the processing use case.
4. Logs failures and retries in subsequent cycles.

Default scheduler interval: 5000 ms.

## 6.6 Ledger Service

Existing functionality:

- Consumes successful payment outcomes.
- Prevents duplicate ledger postings.
- Creates double-entry ledger entries.

Current accounting:

| Account | Entry |
|---|---|
| PROCESSOR_CLEARING | Debit |
| MERCHANT_PAYABLE | Credit |

The current ledger scope does not include refunds, fees, settlement or reconciliation.

---

# 7. Last known verification baseline

As of 6 November 2026, the full Maven verification completed successfully.

Command:

```powershell
mvn clean verify
```

Reported results:

| Module | Tests |
|---|---:|
| API Gateway | 31 |
| Auth | 49 |
| Merchant | 170 |
| Order | 96 |
| Payment | 47 |
| Ledger | 16 |
| **Total** | **409** |

All 9 Maven reactor modules were reported successful.

This is a historical baseline only. It must not be reported as the result of any future code changes.

A previous transient GatewayTimeout 504 test failure passed in isolation, and the subsequent full verification passed.

---

# 8. Active milestone: Manual Risk Review Workflow

**Branch:** `feature/manual-risk-review-workflow`

**Status:** Design and source inspection completed; implementation not yet completed.

The goal is to transform the existing `RISK_REVIEW` state into a secure and auditable human decision workflow.

## 8.1 Functional requirements

### Review queue

Authorized reviewers should be able to retrieve payments awaiting review.

The queue should support practical pagination and should not expose sensitive payment details unnecessarily.

### Review decisions

A reviewer must be able to:

- Approve a payment.
- Reject a payment.

Both operations require a nonblank rationale.

### Reviewer identity

Reviewer identity must be derived from the authenticated JWT subject.

Do not accept a reviewer ID from the request body.

### Audit

Persist:

- Payment ID.
- Reviewer ID.
- Decision.
- Rationale.
- Review timestamp.

The audit must be durable and associated with the payment.

Initially, a payment should have at most one final manual decision.

### Concurrency

Two reviewers must not be able to successfully decide the same payment concurrently.

A database-level conditional transition and unique constraint must enforce this.

### Manual rejection

The following must be atomic:

1. Transition payment to rejected.
2. Persist review audit.
3. Persist `PaymentFailedEvent` in the outbox.

If any step fails, the entire transaction must roll back.

### Manual approval

Manual approval must not invoke the processor directly from the HTTP controller.

It must create a recoverable state that the existing payment recovery mechanism can process.

The recovery mechanism must preserve the existing processor idempotency key.

### Security

Payment Service must enforce authentication and authorization independently of the Gateway.

Only authorized administrators should initially access review operations.

Do not rely solely on Gateway authorization.

---

# 9. Critical architectural decisions for manual review

These decisions must be preserved unless new source inspection reveals a better, justified design.

## 9.1 Keep automated and manual decisions separate

The automated risk fields already exist:

- `riskDecision`
- `riskReasonCode`
- `riskAssessedAt`

These represent the automated risk assessment and must remain unchanged after manual review.

A reviewer decision must be represented separately.

Suggested enum:

```java
public enum ManualReviewDecision {
    APPROVE,
    REJECT
}
```

Do not overwrite an automated REVIEW decision with manual APPROVE or REJECT.

## 9.2 Proposed payment state machine

The intended state progression:

```text
INITIATED
    |
    v
PROCESSING
    |
    +---- Automated APPROVE ----> PROCESSING
    |
    +---- Automated REJECT -----> RISK_REJECTED
    |
    +---- Automated REVIEW -----> RISK_REVIEW
                                      |
                                      +---- Manual APPROVE
                                      |          |
                                      |          v
                                      |     RISK_APPROVED
                                      |          |
                                      |          v
                                      |     Recovery worker
                                      |          |
                                      |          v
                                      |      PROCESSING
                                      |          |
                                      |          v
                                      |      Processor
                                      |
                                      +---- Manual REJECT
                                                 |
                                                 v
                                           RISK_REJECTED
```

`RISK_APPROVED` is a durable intermediate state, not a terminal payment outcome.

## 9.3 Important database constraint issue

V4 currently has risk/status consistency checks.

The existing design must be examined carefully because:

- Automated rejection has `riskDecision = REJECT`.
- Manual rejection has `riskDecision = REVIEW`.
- Both may use `RISK_REJECTED` as the payment status.

A V4 check that insists every `RISK_REJECTED` payment has automated `riskDecision = REJECT` would reject a manually rejected payment.

Do not blindly preserve that incompatible constraint.

The new migration must explicitly address this issue while retaining meaningful integrity checks.

One possible solution is to add separate manual decision metadata to the payment record for operational consistency, while keeping the detailed rationale and audit in a dedicated audit table.

Another possibility is to revise the status consistency constraint to allow the automated REVIEW decision for manually rejected payments, with transactional application-level enforcement and audit uniqueness.

Evaluate both approaches before choosing. Do not silently remove unrelated database constraints.

## 9.4 Manual review audit table

Suggested structure:

`payment_manual_reviews`

Potential columns:

| Column | Purpose |
|---|---|
| review_id | Primary key |
| payment_id | Unique payment reference |
| reviewer_id | Authenticated reviewer UUID |
| decision | APPROVE or REJECT |
| rationale | Mandatory reviewer explanation |
| reviewed_at | Review timestamp |

A unique constraint on payment ID enforces one final decision per payment.

The audit table should not be casually updated or deleted.

## 9.5 Approval recovery

The existing processing service must be updated so a manually approved payment is not held again by the automated risk REVIEW decision.

The scheduler's eligible-payment query must include `RISK_APPROVED`.

The claim operation must atomically transition a due `RISK_APPROVED` payment into `PROCESSING`.

The application processing logic must recognize the persisted manual approval and continue to the processor.

Do not rerun the automated risk assessment or reapply the REVIEW hold.

The existing payment ID idempotency key must remain unchanged.

## 9.6 Concurrency

Do not use a plain JPA load-modify-save operation as the only protection for manual review decisions.

Use an atomic conditional database operation, conceptually:

```sql
UPDATE payments
SET status = ...
WHERE payment_id = ...
  AND status = 'RISK_REVIEW';
```

The implementation must ensure that:

- Only one reviewer wins.
- The audit insert and state transition share one transaction.
- A losing reviewer receives a suitable conflict response.
- No audit row is created for a failed transition.

The exact SQL, transaction ordering and repository design must be checked against the existing adapter and database conventions.

## 9.7 API and route

A suitable initial API design is:

```text
GET  /api/v1/admin/payments/review-queue
POST /api/v1/admin/payments/{paymentId}/review
```

The review request could contain:

```json
{
  "decision": "APPROVE",
  "rationale": "Supporting verification completed."
}
```

Do not accept reviewer identity in the request.

Response codes should distinguish:

- Successful decision.
- Payment not found.
- Payment no longer awaiting review.
- Unauthorized/forbidden.
- Invalid rationale or decision.

The final endpoint design should be checked against existing API conventions before implementation.

Gateway must route the new payment path to Payment Service.

Gateway authorization and Payment Service authorization must both be enforced.

## 9.8 Security dependencies

The last inspection established:

- Payment Service has no current security/API implementation.
- Payment Service does not currently have Spring Security or OAuth2 Resource Server dependencies.
- Gateway uses reactive JWT resource-server security.
- JWTs are HMAC signed.
- The existing roles are USER and ADMIN.
- The JWT subject is a UUID.

Payment Service needs independent JWT validation and ADMIN enforcement.

Reuse the existing JWT signing configuration and compatible token validation settings.

Do not invent a new authentication mechanism or expose a shared signing secret in source control.

---

# 10. Previously inspected source components

The following components were supplied during prior sessions. This inventory is intended to prevent repeated requests.

## Payment domain

- `Payment.java`
- `PaymentStatus.java`
- `RiskDecision.java`
- `PaymentTest.java`

### Payment domain fields

Immutable:

- paymentId
- orderId
- merchantId
- customerId
- sourceEventId
- totalAmount
- createdAt

Mutable:

- status
- updatedAt
- attemptCount
- nextAttemptAt
- processingLeaseUntil
- riskDecision
- riskReasonCode
- riskAssessedAt

The domain has a constructor and rehydration methods with validation.

Existing operations include:

- initiate
- startProcessing
- recordRiskAssessment
- rejectForRisk
- holdForRiskReview
- succeed
- fail
- scheduleRetry

`recordRiskAssessment` is permitted only in PROCESSING and only once.

`holdForRiskReview` requires automated REVIEW.

`rejectForRisk` requires automated REJECT.

Success/failure currently allow automated APPROVE or legacy unassessed payments.

## Payment application

- `ProcessPaymentService`
- `InitiatePaymentService`
- `PaymentRecoveryScheduler`
- `PaymentProcessorPort`
- `PaymentRepository`

`ProcessPaymentService`:

- Claims payment.
- Assesses risk.
- Persists approval before calling processor.
- Holds REVIEW payments.
- Persists rejection and failure outcome.
- Handles retries.
- Handles terminal processor outcomes.
- Uses payment ID as idempotency key.

`InitiatePaymentService`:

- Checks duplicate source events.
- Looks up existing payment by order ID.
- Initiates processing only for eligible existing statuses.
- Creates payment using `createIfAbsent`.

## Payment persistence

- `PaymentJpaEntity`
- `PaymentRepositoryAdapter`
- `SpringDataPaymentJpaRepository`
- `PaymentOutboxJpaEntity`
- `PaymentOutboxJpaRepository`
- `PaymentOutboxRepositoryAdapter`

`PaymentJpaEntity` maps the existing automated risk fields.

`PaymentRepositoryAdapter.saveWithOutcome`:

- Validates terminal event type/status.
- Updates payment.
- Writes the event into outbox.
- Uses a transaction.

`PaymentRepositoryAdapter.save` has no optimistic locking.

The Spring Data claim operation uses a native conditional update.

The processing claim currently supports:

- INITIATED.
- Due RETRYABLE.
- Expired PROCESSING.

It does not yet support RISK_APPROVED.

The due-payment query has the same limitation.

## Migrations

- V1: Payment table.
- V2: Retry and lease fields.
- V3: Payment outbox.
- V4: Automated risk fields and risk status constraints.

Do not modify existing migration files.

## Testing

Previously supplied tests include:

- `InitiatePaymentServiceTest`
- `PaymentOutboxPublisherTest`
- `ProcessPaymentServiceTest`
- `PaymentTest`
- `PaymentInitiationKafkaConsumerTest`
- `PaymentRecoverySchedulerTest`
- `StubPaymentProcessorTest`
- `PaymentPersistenceIntegrationTest`
- `RuleBasedRiskAssessmentAdapterTest`

Payment persistence integration tests use PostgreSQL 16 Testcontainers and Flyway.

Existing persistence tests cover:

- Persist and retrieve by order.
- Retrieve by source event.
- Duplicate order prevention.
- Duplicate source event handling.
- Atomic payment outcome and outbox persistence.
- Rollback of payment changes when outbox insertion fails.

Extend the existing test structure instead of replacing it.

## Auth and Gateway

Previously supplied:

- `Role.java`
- `JwtAuthenticationConverter`
- Auth `SecurityConfiguration`
- `UserController`
- Gateway `SecurityConfiguration`
- Gateway `application.yml`
- JWT token provider.
- Bootstrap admin initialization.

Gateway currently has no Payment Service route.

---

# 11. Required implementation package

The next AI assistant should inspect the current repository once and then provide a complete implementation for the Manual Risk Review Workflow.

The package should include all applicable changes across these layers.

## 11.1 Domain layer

- Manual review decision enum.
- New payment status.
- Manual decision metadata.
- Domain transition methods.
- Validation invariants.
- Rehydration updates.
- Unit tests.

## 11.2 Database and persistence

- New Flyway migration.
- Review audit table.
- Payment operational metadata if required.
- Updated status constraints.
- JPA mapping.
- Repository methods.
- Atomic conditional state transitions.
- Transactional audit persistence.
- Integration tests.

## 11.3 Application layer

- Review queue use case.
- Approve/reject use case.
- Mandatory rationale validation.
- Reviewer ID propagation.
- Conflict handling.
- Recovery compatibility.
- Tests for both success and failure.

## 11.4 API layer

- Review controller.
- Request and response DTOs.
- Queue DTOs.
- Validation.
- Error handling.
- Endpoint documentation.
- Controller tests.

## 11.5 Security

- Payment Service security dependencies.
- JWT resource-server configuration.
- JWT signing/verification configuration.
- ADMIN authorization.
- Reviewer subject extraction.
- Security tests.

## 11.6 Gateway

- Payment Service route.
- Gateway security configuration where needed.
- Route tests.

## 11.7 Processing and recovery

- Recognize manual approval.
- Include RISK_APPROVED in claim logic.
- Include RISK_APPROVED in due-payment discovery.
- Ensure recovery does not reapply automated REVIEW.
- Preserve the original processor idempotency key.
- Add recovery tests.

## 11.8 Events and outbox

- Manual rejection must persist failure outcome atomically.
- Approval must not publish success before processor success.
- Existing outbox publication must remain reliable.
- Add tests for rollback and event publication.

## 11.9 Verification

At minimum:

1. Domain unit tests.
2. Application unit tests.
3. Repository integration tests.
4. Concurrency tests using PostgreSQL.
5. Security tests.
6. Controller tests.
7. Recovery scheduler tests.
8. Payment Service full tests.
9. Full Maven reactor verification.

---

# 12. Mandatory source inspection strategy for the next AI

The developer explicitly dislikes repeated file requests.

Before implementing the milestone:

1. Inspect the repository's current branch and working tree.
2. Inspect all relevant Payment Service production code.
3. Inspect all Payment Service tests.
4. Inspect all Flyway migrations.
5. Inspect Auth JWT implementation and role initialization.
6. Inspect Gateway security and route configuration.
7. Inspect root and service Maven dependencies.
8. Inspect shared event contracts.
9. Inspect application YAML and relevant environment configuration.

Request everything in **one consolidated PowerShell command**.

Do not ask for one file, implement a small portion, then request another file.

Do not request files that have already been supplied in the conversation unless the current working tree may have changed and the exact current content is genuinely needed.

After source inspection, provide the full implementation package in one response wherever possible.

If any unresolved dependency remains, explain it precisely rather than repeatedly requesting unrelated files.

---

# 13. Expected development delivery format

For every milestone, the AI assistant should provide:

### A. Design

- Intended behavior.
- State transitions.
- Database consistency.
- Failure handling.
- Security.
- Important trade-offs.

### B. File change inventory

A table listing:

- File path.
- New or modified.
- Purpose.

### C. Complete code changes

- Full contents for new files.
- Complete replacement contents for modified files.
- Exact migration SQL.
- Maven changes.
- Configuration changes.

Avoid incomplete pseudo-code or snippets that require the user to infer missing methods.

### D. Test plan

- Exact targeted test commands.
- Exact full verification command.
- Expected behavior.
- Known limitations.

### E. Diff review

The developer must review:

```powershell
git status --short
git diff --stat
git diff
```

Only after explicit approval should a commit be made.

### F. Handover update

At milestone completion, update this document with:

- Implementation status.
- Files changed.
- API contracts.
- Database migrations.
- Test counts and actual results.
- Commit hash.
- Remaining limitations.
- Next milestone.

---

# 14. Recommended milestone roadmap

The following is a proposed roadmap, not a claim that all items are scheduled or implemented.

| Milestone | Scope | Status |
|---|---|---|
| Foundation | Repository, shared architecture and core services | Implemented baseline |
| Merchant | Merchant lifecycle and validation | Implemented baseline |
| Order | Order lifecycle and outbox | Implemented baseline |
| Payment | Initiation, risk, processing and recovery | Implemented baseline |
| Ledger | Idempotent successful-payment posting | Implemented baseline |
| Manual Risk Review | Human approval/rejection and audit workflow | In progress / not implemented |
| Fraud/Risk Expansion | More advanced risk evaluation | Planned |
| Bank Integration | External payment/bank integration | Planned |
| Notifications | Customer/merchant notifications | Planned |
| Production Hardening | Observability, deployment, operational resilience | Planned |

The next milestone must be agreed after verifying the completed Manual Risk Review implementation.

---

# 15. Instructions to the next AI assistant

You are continuing an existing engineering project, not starting a new one.

Follow these instructions:

1. Treat this document as the project handover, but verify actual repository state.
2. Do not redesign existing architecture without a concrete reason.
3. Preserve existing event contracts.
4. Preserve existing payment and order semantics.
5. Do not modify historical Flyway migrations.
6. Do not claim production integrations that do not exist.
7. Do not claim exactly-once processing.
8. Do not blindly average architectural complexity into every service.
9. Do not repeatedly request files.
10. Consolidate all source inspection into one command.
11. Deliver complete implementation changes for the agreed milestone.
12. Respect the developer's manual-edit workflow.
13. Do not modify files through scripts unless explicitly requested.
14. Do not commit without approval.
15. Update this document at milestone boundaries.

## Immediate instruction

Continue with:

**Manual Risk Review Workflow — `feature/manual-risk-review-workflow`.**

First verify the repository and inspect the complete relevant implementation surface in one pass. Then provide the full implementation package covering domain, persistence, application, API, security, Gateway, recovery and tests.

Do not ask for individual files in successive iterations.

---

**End of handover document.**