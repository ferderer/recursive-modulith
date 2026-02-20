# Spring Boot Projektstruktur – Architekturdokumentation (arc42)

*Version 1.1 | Februar 2026*

---

## 1. Einführung und Ziele

### 1.1 Aufgabenstellung

Diese Architektur definiert eine wiederverwendbare Package-Struktur für Spring Boot Applikationen. Sie löst das Problem, dass keiner der bestehenden Ansätze (Package-by-Layer, Package-by-Feature, Hexagonal, Modulith) alle Ebenen konsistent adressiert – insbesondere Cross-Cutting Concerns.

Die Struktur ist Grundlage für:
- eine Artikelserie zur Analyse bestehender Ansätze,
- eine Projektvorlage (Referenz-Implementierung),
- einen Custom Spring Initializer (Generator).

### 1.2 Qualitätsziele

| Priorität | Ziel | Maßnahme |
|---|---|---|
| 1 | **Konsistenz** | Rekursives Strukturprinzip auf jeder Ebene (ADR-001) |
| 2 | **Kapselung** | Enge Public API pro BC, package-private als Default (ADR-011, ADR-022) |
| 3 | **Nachvollziehbarkeit** | Jede Klasse hat genau einen Platz; Regeln sind CI-verifizierbar (ADR-019) |
| 4 | **Pragmatismus** | Kein Overengineering; Eskalation nur bei Bedarf (ADR-009, ADR-023) |
| 5 | **Extrahierbarkeit** | BC-Grenzen erlauben spätere Microservice-Extraktion (ADR-022) |

### 1.3 Stakeholder

| Rolle | Erwartung |
|---|---|
| Entwickler (im Team) | Klare Regeln, wo jede Klasse hingehört. Minimaler Diskussionsbedarf. |
| Tech Lead / Architekt | CI-verifizierbare Regeln. Skalierbare Struktur. |
| Neue Teammitglieder | Einheitliches Muster, das schnell erlernbar ist. |
| Generator-Nutzer | Konfigurierbare Struktur per Flags. |

---

## 2. Randbedingungen

### 2.1 Technisch

| Randbedingung | Begründung |
|---|---|
| Java 17+ | Records, sealed classes, pattern matching |
| Spring Boot 3.x | Baseline für Modulith, ArchUnit-Integration |
| Spring Modulith | Modul-Verifikation im CI |
| ArchUnit | Architektur-Regeln als Tests |
| Gradle oder Maven | Build-Tool-agnostisch |

### 2.2 Organisatorisch

| Randbedingung | Begründung |
|---|---|
| Monorepo (ein Artefakt) | Modulith-Ansatz; kein Multi-Module-Build nötig |
| Team-Größe 3–15 | Struktur muss ohne aufwändige Governance funktionieren |
| CI/CD vorhanden | Für automatische Verifikation der Architekturregeln |

### 2.3 Konventionen

| Konvention | Details |
|---|---|
| Package-Namen | Lowercase, keine Underscores, keine Separatoren (ADR-005) |
| Reservierte Namen | `config` und `common` sind reserviert und dürfen nicht als BC-Name verwendet werden |
| Sprache | Package-Namen englisch, Fachbegriffe nach Ubiquitous Language des Projekts |

---

## 3. Kontextabgrenzung

### 3.1 Fachlicher Kontext

Diese Architektur beschreibt die **innere Struktur** einer Spring Boot Applikation. Sie trifft keine Aussagen über:
- externe Schnittstellen (REST-APIs, Messaging, Datenbanken) – diese werden von den BCs definiert,
- Infrastruktur (Deployment, Container, Cloud) – out of scope,
- Frontend-Architektur – out of scope.

### 3.2 Technischer Kontext

```
┌─────────────────────────────────────────────────┐
│              Spring Boot Application            │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ config/  │  │ common/  │  │  {bc}/   │ ...   │
│  │          │  │          │  │          │       │
│  │ Security │  │ Domain   │  │ Service  │       │
│  │ Web      │  │ Error    │  │ Events   │       │
│  │ Error    │  │ Persist. │  │ UseCases │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│       ↑              ↑             ↑            │
│   Framework      Alle BCs     Andere BCs        │
│   only           erlaubt      nur via API       │
└─────────────────────────────────────────────────┘
         ↕              ↕              ↕
    Spring Framework  Shared Types   HTTP / Events
```

---

## 4. Lösungsstrategie

Die Architektur basiert auf fünf Kernentscheidungen:

1. **Rekursives Strukturprinzip (ADR-001):** Dasselbe Schema (`config/` + `common/` + `{fachlich}/`) auf jeder Hierarchieebene. Neue Entwickler lernen ein Muster, das überall gilt.

2. **Zwei-Package-Modell für Cross-Cutting Concerns (ADR-003):** `config/` für Framework-Setup (nicht von fachlichem Code referenzierbar), `common/` für geteilte fachliche Typen (von allen BCs nutzbar).

3. **Single-Action-Controller (ADR-006):** Ein Controller pro Endpunkt mit Logik direkt in der Klasse. Keine künstliche Service-Schicht. Komplexe Domänenlogik im Domänenobjekt (ADR-007).

4. **Enge BC-API (ADR-022):** Nur `{Bc}Service` und Events sind von außen sichtbar. Alles andere ist package-private. Durchgesetzt via Modulith `verify()` und ArchUnit.

5. **Eskalation statt Overengineering (ADR-009, ADR-023):** Flache Struktur als Default. Sub-Packages, Ressourcen-Gruppierung und Sub-BCs erst bei messbarer Komplexität.

---

## 5. Bausteinsicht

### 5.1 Ebene 1 – Top-Level

```
com.acme.app
├── Application.java
├── config/                     ← Framework-Konfiguration (ADR-002)
├── common/                     ← App-weiter geteilter Code (ADR-003, ADR-004)
├── {bc-a}/                     ← Bounded Context A
├── {bc-b}/                     ← Bounded Context B
└── {bc-c}/                     ← Bounded Context C
```

| Baustein | Verantwortung | Zugriff |
|---|---|---|
| `config/` | Framework-Setup: Security, Web, Error-Handling, Persistence, OpenAPI, Messaging, Observability | Nur vom Framework. Fachlicher Code darf nicht importieren. |
| `common/` | Geteilte Value Objects, Error-Enums, BaseEntity, Utilities | Von allen BCs. |
| `{bc}/` | Bounded Context mit eigener Domäne, Use Cases, Persistenz | Nur über Public API (`{Bc}Service`, Events). |

### 5.2 Ebene 2 – `config/`

```
config/
├── security/
│   ├── SecurityConfig.java          FilterChains, @Order
│   └── JwtTokenService.java
├── web/
│   ├── CorsConfig.java
│   └── JacksonConfig.java
├── error/
│   └── GlobalExceptionHandler.java  @ControllerAdvice → ProblemDetail
├── persistence/
│   └── AuditingConfig.java          AuditorAware
├── openapi/
│   └── OpenApiConfig.java
├── messaging/                       Kafka/RabbitMQ (optional)
└── observability/                   Micrometer, Tracing (optional)
```

`GlobalExceptionHandler` liegt unter `config/error/`, weil er Framework-Konfiguration ist: Er sagt Spring, wie Exceptions in HTTP-Responses übersetzt werden (ADR-002, ADR-015).

### 5.3 Ebene 2 – `common/`

```
common/
├── Tsid.java                        Einzelne Utility-Klasse, kein Sub-Package nötig
├── domain/
│   ├── Money.java                   Value Object
│   ├── Address.java                 Value Object
│   └── Currency.java                Enum
├── error/
│   └── AppError.java                Guard4j Error-Enum (UNAUTHORIZED, NOT_FOUND, ...)
├── persistence/
│   └── BaseEntity.java              id, createdAt, updatedAt
└── validation/
    └── ...                          Custom Validators, Annotations
```

**Litmustest für Platzierung in `common/` (ADR-004):**
- Von >50% der BCs genutzt → `common/` (App-Ebene).
- Von 2–3 BCs genutzt → `common/` der übergeordneten Ebene oder eigenes Modul.
- Von 1 BC genutzt → gehört IN diesen BC.

### 5.4 Ebene 2 – Bounded Context

```
{bc}/
├── {Bc}Service.java                 Public API (Fassade, keine Geschäftslogik) (ADR-010)
├── {Bc}CreatedEvent.java            Public Event (optional)
├── common/
│   ├── domain/
│   │   ├── Thing.java               Domänenklasse / DTO (kein Postfix)
│   │   ├── ThingEntity.java         JPA-Entität (ADR-020)
│   │   └── ThingStatus.java         Enum
│   ├── error/
│   │   └── {Bc}Error.java           Guard4j Error-Enum (ADR-015)
│   ├── events/                      Ab >3 publizierte Events (ADR-016)
│   │   └── ...
│   └── persistence/
│       └── ThingRepository.java     Wenn ≥2 Use Cases nutzen (ADR-014)
├── {usecase-a}/                     Use Case Package
└── {usecase-b}/                     Use Case Package
```

**Public API eines BC (ADR-022):** Nur `{Bc}Service` und Events dürfen von anderen BCs referenziert werden. Alles andere ist package-private. Durchgesetzt durch Modulith `verify()`.

**`{Bc}Service` (ADR-010):** Reine Fassade. Orchestriert Use Cases, enthält keine Geschäftslogik. Geteilte Domänenlogik liegt in `{bc}/common/domain/` (ADR-017).

### 5.5 Ebene 3 – Use Case (Single-Action-Controller)

**Default: Flach (ADR-006)**

```
creation/
├── CreatePolicyDraft.java           POST /api/v1/policies/drafts
├── GetPolicyDraft.java              GET  /api/v1/policies/drafts/{id}
├── SubmitPolicyDraft.java           POST /api/v1/policies/drafts/{id}/submit
├── PolicyDraft.java                 Domänenklasse (Use-Case-lokal)
├── PolicyDraftEntity.java           JPA-Entität (Use-Case-lokal)
└── PolicyDraftRepository.java       Repository (wenn nur 1 Use Case nutzt)
```

**Endpunkt-Klasse (ADR-006, ADR-007):**

```java
@RestController
@RequestMapping("/api/v1/policies/drafts")
@Transactional                                      // ADR-012
class CreatePolicyDraft {

    record Request(String holderName, Coverage coverage) {}
    record Response(UUID id, String holderName, Status status) {}

    private final PolicyDraftRepository repo;
    private final TsidGenerator tsid;

    @PostMapping
    Response handle(@RequestBody Request req) {
        var draft = PolicyDraft.create(                // Logik im Domänenobjekt (ADR-007)
            tsid.next(), req.holderName(), req.coverage());
        repo.save(draft);
        return new Response(draft.id(), draft.holderName(), draft.status());
    }
}
```

**Eskaliert: Sub-Package (ADR-009)**

Trigger: Mapper, Validator oder ≥3 Klassen.

```
creation/
├── CreatePolicyDraft.java           Einfach, bleibt flach
├── GetPolicyDraft.java
├── submitpolicydraft/               Komplex → eigenes Package
│   ├── SubmitPolicyDraft.java
│   ├── SubmitValidator.java
│   └── UnderwritingResult.java
├── PolicyDraft.java
└── PolicyDraftRepository.java
```

**Service extrahieren (ADR-006):** Erst wenn die Logik von mehreren Stellen aufgerufen wird (zweiter Controller, Event-Listener, Scheduled Job).

### 5.6 Ebene 3 – Listener als Use Case (ADR-016)

Event-Listener sind Use Cases. Sie liegen im Use-Case-Package, nicht in einem `listeners/`-Package.

```
claims/
├── common/
├── filing/
│   ├── FileClaim.java                 HTTP-Trigger
│   └── ...
└── policycancelled/
    └── HandlePolicyCancelled.java     Event-Trigger
```

---

## 6. Laufzeitsicht

### 6.1 Use-Case-Aufruf (synchron)

```
HTTP Request
    → CreatePolicyDraft.handle()          @Transactional
        → PolicyDraft.create(...)         Domänenlogik
        → PolicyDraftRepository.save()
    ← HTTP Response
```

Keine Zwischenschicht. Transaktion auf Use-Case-Ebene (ADR-012).

### 6.2 Cross-BC-Kommunikation (synchron)

```
Designer BC                              Plugin BC
    CreateWorkflow.handle()
        → PluginRegistryService          Public API
            .findCompatible(type, caps)
        ← List<PluginSummary>
```

Direkter Service-Call wenn sofortige Antwort nötig (ADR-013).

### 6.3 Cross-BC-Kommunikation (asynchron)

```
Policy BC                                Claims BC
    ActivatePolicy.handle()
        → ApplicationEventPublisher
            .publishEvent(PolicyActivatedEvent)

                                         HandlePolicyActivated.on(event)
                                             → ClaimsService.enableFilingFor(...)
```

Spring Event für Fire-and-Forget (ADR-013). Modulith Event Publication Registry gewährleistet Transaktionssicherheit.

### 6.4 Fehlerfall

Error-Enums definieren Fehlercode + HTTP-Status + Message-Template (ADR-015). `GlobalExceptionHandler` in `config/error/` übersetzt (ADR-002).

---

## 7. Verteilungssicht

Nicht Gegenstand dieser Architektur. Die Struktur ist für ein einzelnes Spring Boot Artefakt (Modulith) ausgelegt. BC-Grenzen ermöglichen spätere Extraktion zu Microservices (ADR-022): `{Bc}Service` → REST-Client, Events → Message Broker.

---

## 8. Querschnittliche Konzepte

### 8.1 Cross-Cutting Concerns: Drei Kategorien

| # | Kategorie | Package | Beispiele |
|---|---|---|---|
| 1 | Framework-Konfiguration | `config/` | Security, Web, Error-Handling, Persistence |
| 2 | Geteilte fachliche Klassen | `common/` (jede Ebene) | Value Objects, Error-Enums, BaseEntity |
| 3 | Geteilter Supporting-Code | `common/` (jede Ebene) | Utilities, ID-Generierung, Validators |

Kategorien 2 und 3 können auf jeder Hierarchieebene vorkommen (ADR-004). Kategorie 1 nur auf Top-Level (ADR-002).

### 8.2 Error Handling (ADR-015)

Drei Schichten:

| Schicht | Ort | Inhalt |
|---|---|---|
| App-weite Errors | `common/error/AppError.java` | Guard4j Error-Enum: UNAUTHORIZED, NOT_FOUND, ... |
| BC-spezifische Errors | `{bc}/common/error/{Bc}Error.java` | Guard4j Error-Enum: POLICY_EXPIRED, INVALID_COVERAGE, ... |
| Exception → HTTP | `config/error/GlobalExceptionHandler.java` | `@ControllerAdvice`: GuardException → ProblemDetail |

Separate Exception-Klassen nur bei gezieltem `catch` (z.B. Retry-Logik).

### 8.3 Transaktionen (ADR-012)

`@Transactional` gehört auf die Use-Case-Klasse.

| Typ | Annotation |
|---|---|
| Schreibender Use Case | `@Transactional` |
| Lesender Use Case | `@Transactional(readOnly = true)` |
| Event Listener (schreibend) | `@Transactional` |
| `{Bc}Service` | Keine (delegiert nur) |

**Escape-Hatches:** Batch-Verarbeitung, Outbox-Pattern, SAGA-Orchestratoren dürfen abweichen. Muss dokumentiert und in Code-Reviews bestätigt werden. Spring Batch regelt Transaktionsgrenzen eigenständig.

### 8.4 Naming-Konventionen (ADR-008, ADR-020)

| Postfix | Verwendung | Beispiel |
|---|---|---|
| *(keiner)* | Domänenklasse, DTO, Value Object | `PolicyDraft`, `Money` |
| *(keiner)* | Endpunkt-Klasse (Aktionsname) | `CreatePolicyDraft` |
| `Entity` | JPA-annotierte Persistenzklasse | `PolicyDraftEntity` |
| `Repository` | Spring Data Interface | `PolicyDraftRepository` |
| `Service` | Geteilte Geschäftslogik / BC Public API | `PolicyService` |
| `Error` | Guard4j Error-Enum | `PolicyError` |

Kein `Controller`-Postfix. Kein `Dto`-Postfix. Fachliche Namen bleiben sauber.

### 8.5 Sichtbarkeit (ADR-011)

**Default:** Package-private.

**Public für Cross-BC-Zugriff:** `{Bc}Service`, Events.

**Public (technisch notwendig):** Use-Case-Klassen, Entities, Repositories – aber kein Cross-Use-Case-Zugriff erlaubt.

**Regel:** Kein Code außerhalb des Use-Case-Packages darf einen Use Case direkt referenzieren.

### 8.6 Abhängigkeitsregeln

```
                    ┌──────────┐
                    │ config/  │ ← Nur Framework referenziert
                    └──────────┘
                         ↑ verboten
                    ┌──────────┐
              ┌────▶│ common/  │◀────┐
              │     └──────────┘     │
              │                      │
         ┌────┴────┐           ┌────┴────┐
         │  bc-a/  │──────────▶│  bc-b/  │
         └─────────┘  nur via  └─────────┘
                     Service/
                     Events
```

| Regel | Durchsetzung |
|---|---|
| Fachlicher Code darf `config/` nicht importieren | ArchUnit |
| BC darf nur auf `{OtherBc}Service` + Events zugreifen | Modulith verify() |
| Use Case darf keinen anderen Use Case referenzieren | ArchUnit |
| Kein `@Transactional` außerhalb von Use-Case-Klassen | ArchUnit |

---

## 9. Architekturentscheidungen

Alle Entscheidungen sind als ADRs dokumentiert. Die folgende Tabelle gibt einen Überblick:

| ADR | Entscheidung | Status |
|---|---|---|
| ADR-001 | Rekursives Strukturprinzip | Accepted |
| ADR-002 | `config/` nur auf Top-Level | Accepted |
| ADR-003 | Zwei-Package-Modell (`config` + `common`) | Accepted |
| ADR-004 | `common/` auf jeder Hierarchieebene | Accepted |
| ADR-005 | Package-Namen ohne Underscore | Accepted |
| ADR-006 | Single-Action-Controller als Default | Accepted |
| ADR-007 | Domänenlogik gehört ins Domänenobjekt | Accepted |
| ADR-008 | Naming-Konventionen (kein Controller/Dto-Postfix) | Accepted |
| ADR-009 | Action-Package-Eskalation (Mapper/Validator/≥3 Klassen) | Accepted |
| ADR-010 | `{Bc}Service` als reine Fassade | Accepted |
| ADR-011 | Sichtbarkeitsregeln (package-private Default) | Accepted |
| ADR-012 | Transaktionen auf Use-Case-Ebene | Accepted |
| ADR-013 | Cross-BC-Kommunikation (Service-Call vs. Event) | Accepted |
| ADR-014 | Repository-Platzierung (1 UC → lokal, ≥2 → common) | Accepted |
| ADR-015 | Error Handling mit Guard4j | Accepted |
| ADR-016 | Listener als Use Case | Accepted |
| ADR-017 | Geteilte Domänenlogik in `{bc}/common/domain/` | Accepted |
| ADR-018 | Keine `application/` + `domain/` Sub-Packages | Rejected |
| ADR-019 | Modulith-Integration + ArchUnit-Regeln | Accepted |
| ADR-020 | Entity-Postfix für JPA, saubere Namen für Domäne | Accepted |
| ADR-021 | Mehrere Aggregate pro BC erlaubt | Accepted |
| ADR-022 | BC-Grenze durch Public API | Accepted |
| ADR-023 | Hierarchische Eskalation bei wachsenden BCs | Accepted |

Vollständige ADRs mit Kontext, Begründung und verworfenen Alternativen: siehe *Architecture Decision Records* (separates Dokument).

---

## 10. Qualitätsanforderungen

### 10.1 Qualitätsbaum

```
Wartbarkeit
├── Konsistenz ──────── Rekursives Prinzip, ein Muster für alles
├── Kapselung ───────── Package-private Default, enge BC-API
├── Auffindbarkeit ──── Jede Klasse hat genau einen Platz
└── Verifizierbarkeit ─ ArchUnit + Modulith verify() im CI

Erweiterbarkeit
├── Skalierung ──────── Eskalationsregeln für wachsende BCs
├── Extrahierbarkeit ── BC-Grenzen = Microservice-Grenzen
└── Flexibilität ────── Generator-Flags für Projekt-spezifische Anpassungen

Erlernbarkeit
├── Einstiegshürde ──── Niedrig (ein rekursives Muster)
└── Onboarding ──────── Struktur ist selbstdokumentierend
```

### 10.2 Qualitätsszenarien

| # | Szenario | Maßnahme | Messung |
|---|---|---|---|
| Q1 | Neuer Entwickler findet in <5 Min heraus, wo eine neue Klasse hingehört | Rekursives Prinzip + Naming-Konventionen | Onboarding-Feedback |
| Q2 | Änderung in BC-A hat keinen Einfluss auf BC-B | Enge Public API + Modulith verify() | Modulith-Test schlägt bei Verstoß fehl |
| Q3 | BC mit >60 Klassen bleibt navigierbar | Hierarchische Eskalation (ADR-023) | Keine Packages mit >30 Dateien |
| Q4 | Team hält Architekturregeln ein, auch unter Zeitdruck | ArchUnit im CI, Build bricht bei Verstoß | 0 Architektur-Violations in CI |
| Q5 | BC kann zu Microservice extrahiert werden | Service → REST-Client, Events → Broker | Extraktion ohne interne Refactorings möglich |

---

## 11. Risiken und technische Schulden

| Risiko | Wahrscheinlichkeit | Auswirkung | Maßnahme |
|---|---|---|---|
| Single-Action-Controller ungewohnt für Teams | Hoch | Widerstand, Rückfall in Service-Layer | ADR-006/007 als Onboarding-Material, Code-Reviews |
| `common/` wächst unkontrolliert (Gravity Well) | Mittel | Kopplung steigt | Litmustest (ADR-004), regelmäßige Reviews |
| Guard4j als externe Abhängigkeit | Niedrig | Vendor Lock-in | Error-Enum-Pattern ist auch ohne Guard4j implementierbar |
| ArchUnit-Regeln zu strikt → Workarounds | Mittel | Regeln werden umgangen statt eingehalten | Escape-Hatches dokumentieren (ADR-012), Regeln iterativ anpassen |
| Package-private Klassen erschweren Testbarkeit | Mittel | Tests müssen im selben Package liegen | Teststruktur spiegelt Hauptstruktur, Spring-Test-Utilities nutzen |

---

## 12. Glossar

| Begriff | Definition | Package-Mapping |
|---|---|---|
| **Bounded Context (BC)** | Modell mit klarer Grenze. „Kunde" in Billing ≠ „Kunde" in CRM. | Top-Level-Package unter Root |
| **Use Case** | Eine konkrete fachliche Operation, ausgelöst durch HTTP oder Event. | Sub-Package innerhalb eines BC |
| **Single-Action-Controller** | Eine Controller-Klasse mit genau einer Handler-Methode und Logik. | Klasse im Use-Case-Package |
| **Public API (eines BC)** | `{Bc}Service` + Events. Einziger Zugang von außen. | Klassen im BC-Root |
| **`config/`** | Framework-Konfiguration. Nicht von fachlichem Code referenzierbar. | Top-Level-Package |
| **`common/`** | Geteilter Code einer Hierarchieebene. Rekursiv anwendbar. | Package auf jeder Ebene |
| **Action-Package-Eskalation** | Auslagerung eines Endpunkts in ein eigenes Sub-Package bei Komplexität. | Sub-Package im Use-Case-Package |
| **Hierarchische Eskalation** | Einführung einer Zwischenebene (Sub-BC, Ressourcen-Gruppe) bei >60–80 Klassen. | Neue Package-Ebene |
| **Guard4j Error-Enum** | Enum, das Fehlercode, HTTP-Status und Message-Template definiert. | `{bc}/common/error/` |
| **Aggregate** | Cluster von Domänenobjekten mit gemeinsamer Konsistenzgrenze. | Klassen in `{bc}/common/domain/` oder Use-Case-Package |

---

## Anhang A: Vollständiges Beispiel

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
│   ├── PolicyService.java                  ← Public API
│   ├── PolicyActivatedEvent.java           ← Public Event
│   ├── common/
│   │   ├── domain/
│   │   │   ├── PolicyDraft.java
│   │   │   ├── PolicyDraftEntity.java
│   │   │   └── PolicyStatus.java
│   │   ├── error/
│   │   │   └── PolicyError.java
│   │   ├── events/
│   │   │   ├── PolicyCreatedEvent.java
│   │   │   ├── PolicyActivatedEvent.java
│   │   │   ├── PolicyExpiredEvent.java
│   │   │   └── PolicyCancelledEvent.java
│   │   └── persistence/
│   │       └── PolicyDraftRepository.java
│   ├── creation/
│   │   ├── CreatePolicyDraft.java
│   │   ├── GetPolicyDraft.java
│   │   └── submitpolicydraft/
│   │       ├── SubmitPolicyDraft.java
│   │       ├── SubmitValidator.java
│   │       └── UnderwritingResult.java
│   └── renewal/
│       ├── RenewPolicy.java
│       └── RenewalEligibilityCheck.java
│
├── claims/                                ← Bounded Context
│   ├── ClaimsService.java
│   ├── common/
│   │   ├── domain/
│   │   │   ├── Claim.java
│   │   │   └── ClaimEntity.java
│   │   ├── error/
│   │   │   └── ClaimsError.java
│   │   └── persistence/
│   │       └── ClaimRepository.java
│   ├── filing/
│   │   ├── FileClaim.java
│   │   └── GetClaim.java
│   ├── assessment/
│   │   └── AssessClaim.java
│   └── policycancelled/
│       └── HandlePolicyCancelled.java     ← Listener = Use Case
│
└── billing/                               ← Bounded Context
    ├── BillingService.java
    ├── common/
    │   └── error/
    │       └── BillingError.java
    ├── invoice/
    │   ├── CreateInvoice.java
    │   └── GetInvoice.java
    └── payment/
        ├── ProcessPayment.java
        └── RefundPayment.java
```

---

## Anhang B: Stresstest – Workflow Designer BC

Mapping des Workflow Designer (42 User Stories, 5 Screens) auf die Struktur:

```
designer/
├── DesignerService.java
├── common/
│   ├── domain/
│   │   ├── Workflow.java
│   │   ├── WorkflowEntity.java
│   │   ├── Stage.java
│   │   ├── StageEntity.java
│   │   ├── PluginRef.java
│   │   ├── StageConnection.java
│   │   └── WorkflowStatus.java
│   ├── error/
│   │   └── DesignerError.java
│   └── persistence/
│       ├── WorkflowRepository.java
│       └── StageRepository.java
├── overview/
│   ├── ListWorkflows.java
│   ├── CreateWorkflow.java
│   ├── DuplicateWorkflow.java
│   └── DeleteWorkflow.java
├── pipeline/
│   ├── GetPipeline.java
│   ├── AddStage.java
│   ├── RemoveStage.java
│   ├── ReorderStages.java
│   └── SetStageTransition.java
├── stageconfig/
│   ├── GetStageDetail.java
│   ├── SetSource.java
│   ├── AddProcessor.java
│   ├── RemoveProcessor.java
│   ├── ReorderProcessors.java
│   ├── AddSink.java
│   ├── RemoveSink.java
│   ├── SetExecutor.java
│   └── SavePluginConfig.java
├── metadata/
│   ├── UpdateMetadata.java
│   ├── ManageTags.java
│   ├── GetVersionHistory.java
│   ├── RestoreVersion.java
│   └── ActivateWorkflow.java
└── transfer/
    ├── ExportWorkflow.java
    └── ImportWorkflow.java
```

**Beobachtungen aus dem Stresstest:**
- `common/domain/` enthält 7 Klassen (Aggregate Root + Value Objects) – akzeptabel für einen BC dieser Größe.
- Repositories in `common/persistence/`, weil fast jeder Use Case `WorkflowRepository` braucht (ADR-014).
- Plugin-Auswahl und Connection-Test gehören in den Plugin-BC (Cross-BC, ADR-013).
- Auto-Save, Undo/Redo sind Frontend-Concerns – Backend sieht normale Requests.

---

## Anhang C: Eskalationsschwellen (ADR-023)

| Signal | Schwelle | Aktion |
|---|---|---|
| Use Cases pro BC | >25–30 | Ressourcen-Gruppierung als Zwischenebene |
| Klassen pro BC | >60–80 | Sub-BC erwägen |
| Aggregate pro BC | >12–15 | Sub-BC erwägen |
| ArchUnit-Zyklen | Jeder | Sofort auflösen |
| Merge-Konflikte | Häufig im selben BC | Starkes Signal für Aufteilung |

Neue Ebene folgt dem rekursiven Prinzip. Eigene Aggregate → Sub-BC mit eigenem `common/`. Nur Gruppierung → kein `common/` nötig.
