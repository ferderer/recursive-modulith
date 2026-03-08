# Recursive-Modulith
*– Architecture is perfect when nothing can be taken away.*

**A pragmatic, recursive package structure for Spring Boot modular monoliths.**

Also known as **Matryoshka Architecture** 🪆 — because every level contains the same pattern, just smaller.

---

## The Problem

Spring Boot has no official guidance for package structures beyond tutorials. Existing approaches each solve part of the puzzle:

| Approach | Strength | Weakness |
|---|---|---|
| Package-by-Layer | Easy to start | No cohesion, no encapsulation |
| Package-by-Feature | High cohesion | No answer for cross-cutting concerns |
| Hexagonal / Clean | Strong boundaries | Massive boilerplate, over-engineered for most projects |
| Spring Modulith | Module verification | No guidance for internal structure |

**None of them address all levels consistently. That's the gap this project fills.**

## The Solution

One recursive pattern, applied at every level:

```
{level}/
├── config/        ← framework setup (top-level only)
├── common/        ← shared code (any level)
└── {domain}/      ← bounded context, use case, sub-module
```

App → Bounded Context → Use Case → Action — same structure, all the way down.

## Quick Reference

```
config/       → Framework setup. Top-level only. Never import from domain code.
common/       → Shared code. Any level. Visible downward.
{bc}/         → Bounded context. Public API = Service + Events.
  common/     → BC-internal shared code. domain/, error/, persistence/.
  {usecase}/  → One endpoint = one class. No service layer.
    Class     → Action name. Request/Response as inner records.
    Entity    → JPA. Postfix "Entity". Domain class without postfix.
```

## Full Example

```
com.acme.insuranceapp
├── Application.java
│
├── config/
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   └── JwtTokenService.java
│   ├── web/
│   │   ├── CorsConfig.java
│   │   └── JacksonConfig.java
│   ├── error/
│   │   └── GlobalExceptionHandler.java
│   ├── persistence/
│   │   └── AuditingConfig.java
│   └── openapi/
│       └── OpenApiConfig.java
│
├── common/
│   ├── Tsid.java
│   ├── domain/
│   │   ├── Money.java
│   │   ├── Address.java
│   │   └── Currency.java
│   ├── error/
│   │   └── AppError.java
│   └── persistence/
│       └── BaseEntity.java
│
├── policy/                                ← Bounded Context
│   ├── PolicyService.java                  ← Public API (facade only)
│   ├── PolicyActivatedEvent.java           ← Public Event
│   ├── common/
│   │   ├── domain/
│   │   │   ├── PolicyDraft.java            ← Domain class (clean name)
│   │   │   ├── PolicyDraftEntity.java      ← JPA entity (postfix)
│   │   │   └── PolicyStatus.java
│   │   ├── error/
│   │   │   └── PolicyError.java            ← Guard4j error enum
│   │   └── persistence/
│   │       └── PolicyDraftRepository.java  ← shared by ≥2 use cases
│   ├── creation/
│   │   ├── CreatePolicyDraft.java          ← POST endpoint
│   │   ├── GetPolicyDraft.java             ← GET endpoint
│   │   └── submitpolicydraft/              ← escalated (complex)
│   │       ├── SubmitPolicyDraft.java
│   │       ├── SubmitValidator.java
│   │       └── UnderwritingResult.java
│   └── renewal/
│       └── RenewPolicy.java
│
├── claims/                                ← Bounded Context
│   ├── ClaimsService.java
│   ├── common/
│   │   ├── error/
│   │   │   └── ClaimsError.java
│   │   └── persistence/
│   │       └── ClaimRepository.java
│   ├── filing/
│   │   ├── FileClaim.java
│   │   └── GetClaim.java
│   └── policycancelled/
│       └── HandlePolicyCancelled.java     ← Event listener = use case
│
└── billing/
    ├── BillingService.java
    ├── common/
    │   └── error/
    │       └── BillingError.java
    ├── invoice/
    └── payment/
```

## What a Use Case Looks Like

One endpoint, one class, no service layer:

```java
@RestController
@RequestMapping("/api/v1/policies/drafts")
@Transactional
class CreatePolicyDraft {

    record Request(String holderName, Coverage coverage) {}
    record Response(UUID id, String holderName, Status status) {}

    private final PolicyDraftRepository repo;
    private final TsidGenerator tsid;

    @PostMapping
    Response handle(@RequestBody Request req) {
        var draft = PolicyDraft.create(tsid.next(), req.holderName(), req.coverage());
        repo.save(draft);
        return new Response(draft.id(), draft.holderName(), draft.status());
    }
}
```

Extract a service **only when** a second caller appears.

## Key Rules

### Dependency Rules

| Rule | Enforcement |
|---|---|
| Domain code must not import `config.*` | ArchUnit |
| BC-to-BC access only via `{Bc}Service` or Events | Modulith verify() |
| No direct use-case-to-use-case references | ArchUnit |
| `@Transactional` only on use-case classes | ArchUnit |

### Naming Conventions

| Postfix | When | Example |
|---|---|---|
| *(none)* | Domain class, DTO, value object | `PolicyDraft`, `Money` |
| *(none)* | Endpoint (action name) | `CreatePolicyDraft` |
| `Entity` | JPA class | `PolicyDraftEntity` |
| `Repository` | Spring Data | `PolicyDraftRepository` |
| `Service` | BC public API (facade) | `PolicyService` |
| `Error` | Guard4j error enum | `PolicyError` |

No `Controller` postfix. No `Dto` postfix.

### When to Escalate

| Signal | Threshold | Action |
|---|---|---|
| Classes in use-case package | Mapper/Validator or ≥3 | Sub-package for endpoint |
| Use cases per BC | >25–30 | Resource grouping |
| Classes per BC | >60–80 | Consider sub-BC |
| Aggregates per BC | >12–15 | Consider sub-BC |
| ArchUnit cycles | Any | Resolve immediately |

### Error Handling (3 Layers)

```
common/error/AppError.java              ← App-wide errors (Guard4j enum)
{bc}/common/error/{Bc}Error.java        ← BC-specific errors (Guard4j enum)
config/error/GlobalExceptionHandler.java ← Exception → ProblemDetail mapping
```

### CI Verification

```java
@Test
void verifyModulithStructure() {
    ApplicationModules.of(Application.class).verify();
}
```

## Documentation

| Document | Purpose |
|---|---|
| [Architecture Decision Records](docs/adrs/) | All 23 ADRs with context, decision, rationale |
| [arc42 Documentation](docs/arc42.md) | Full architecture documentation |
| [Ruleset](docs/regelwerk.md) | Practical reference (German) |
| [Analysis](docs/analyse.md) | Comparison of existing approaches |

## Why "Matryoshka"?

Like Russian nesting dolls:

- 🪆 Every doll has the **same shape** → every level follows `config/` + `common/` + `{domain}/`
- 🪆 Dolls are **nested inside each other** → App → [Domain] → [Subdomain] → Bounded Context → Use Case → Action
- 🪆 Each doll is **self-contained** → every BC is extractable to a microservice
- 🪆 From outside, you only see the **outer shell** → public API

## Status

- [x] Architecture analysis & comparison
- [x] Architecture Decision Records (ADR-001 to ADR-023)
- [x] arc42 documentation
- [x] Practical ruleset
- [ ] Reference implementation
- [ ] Custom Spring Initializer (generator)
- [ ] Article series

## License

[MIT](LICENSE)
