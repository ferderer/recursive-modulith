# Spring Boot Projektstruktur – Regelwerk

*Praktische Anleitung. Begründungen siehe ADRs.*

---

## 1. Das Grundmuster

Jede Ebene folgt demselben Schema:

```
{ebene}/
├── config/        ← nur Top-Level
├── common/        ← jede Ebene erlaubt
└── {fachlich}/    ← Bounded Context, Use Case, Sub-BC
```

---

## 2. Top-Level

```
com.acme.app
├── Application.java
├── config/          Framework-Setup. Fachlicher Code darf NICHT importieren.
├── common/          App-weit geteilter Code. Alle BCs dürfen zugreifen.
├── {bc-a}/          Bounded Context
└── {bc-b}/          Bounded Context
```

### `config/` – Was gehört rein?

`security/`, `web/`, `error/` (GlobalExceptionHandler), `persistence/`, `openapi/`, `messaging/`, `observability/`

### `common/` – Was gehört rein?

| Sub-Package | Inhalt |
|---|---|
| `domain/` | Value Objects (Money, Address), Enums |
| `error/` | `AppError.java` (Guard4j Error-Enum) |
| `persistence/` | `BaseEntity.java` |
| `validation/` | Custom Validators |
| *(Top-Level)* | Einzelne Utilities (z.B. `Tsid.java`) |

### Litmustest: Gehört es in `common`?

- \>50% der BCs nutzen es → ja, App-Level `common/`
- 2–3 BCs → `common/` der übergeordneten Ebene
- 1 BC → gehört IN diesen BC
- Eigener Lifecycle → eigenes Modul

---

## 3. Bounded Context

```
{bc}/
├── {Bc}Service.java          ← Public API (Fassade, KEINE Geschäftslogik)
├── {Bc}XxxEvent.java         ← Public Events
├── common/
│   ├── domain/               Domänenklassen, Entities, Enums
│   ├── error/                {Bc}Error.java (Guard4j)
│   ├── events/               Ab >3 Events
│   └── persistence/          Repositories (wenn ≥2 Use Cases nutzen)
├── {usecase-a}/
└── {usecase-b}/
```

### Public API

Andere BCs dürfen NUR zugreifen auf:
- `{Bc}Service`
- Events

Alles andere: package-private.

### `{Bc}Service`

- Reine Fassade. Orchestriert, berechnet nicht.
- Geteilte Logik → `{bc}/common/domain/` (z.B. `PolicyNumberGenerator.java`)

### Repository-Platzierung

| Fall | Ort |
|---|---|
| 1 Use Case nutzt es | Im Use-Case-Package |
| ≥2 Use Cases | `{bc}/common/persistence/` |
| BC-übergreifend | Verboten. Zugriff über `{Bc}Service`. |

---

## 4. Use Case: Single-Action-Controller

**Ein Endpunkt = eine Klasse. Keine Service-Schicht.**

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

### Regeln

- Request/Response als `static` Inner Records. Kein `Dto`-Postfix.
- Domänenlogik ins Domänenobjekt, nicht in den Controller.
- `@Transactional` auf der Klasse (schreibend) oder `@Transactional(readOnly = true)` (lesend).
- Service extrahieren **erst wenn** mehrere Aufrufer existieren.
- Listener sind Use Cases (kein `listeners/`-Package).

### Eskalation: Wann Sub-Package?

**Trigger:** Mapper, Validator oder ≥3 Klassen.

```
creation/
├── CreatePolicyDraft.java            einfach → bleibt flach
├── GetPolicyDraft.java
├── submitpolicydraft/                komplex → eigenes Package
│   ├── SubmitPolicyDraft.java
│   ├── SubmitValidator.java
│   └── UnderwritingResult.java
└── PolicyDraftRepository.java
```

---

## 5. Naming

| Postfix | Wann | Beispiel |
|---|---|---|
| *(keiner)* | Domänenklasse, DTO, Value Object | `PolicyDraft`, `Money` |
| *(keiner)* | Endpunkt (Aktionsname) | `CreatePolicyDraft` |
| `Entity` | JPA-Klasse | `PolicyDraftEntity` |
| `Repository` | Spring Data | `PolicyDraftRepository` |
| `Service` | BC Public API | `PolicyService` |
| `Error` | Guard4j Error-Enum | `PolicyError` |

Kein `Controller`-Postfix. Kein `Dto`-Postfix. Packages: lowercase, keine Underscores.

---

## 6. Error Handling

```
common/error/AppError.java              ← App-weite Fehler
{bc}/common/error/{Bc}Error.java        ← BC-spezifische Fehler
config/error/GlobalExceptionHandler.java ← Exception → ProblemDetail
```

```java
public enum PolicyError implements ErrorCode {
    POLICY_EXPIRED(410, "Policy {0} has expired"),
    INVALID_COVERAGE(400, "Invalid coverage: {0}");
    // ...
}
```

Separate Exception-Klassen nur bei gezieltem `catch`.

---

## 7. Abhängigkeiten

| Regel | Kurzform |
|---|---|
| `config/` | Nicht importieren. Nie. |
| BC → BC | Nur über `{Bc}Service` oder Events |
| Use Case → Use Case | Verboten. Über `{Bc}Service` oder Events. |
| `common/` | Jeder darf auf sein Level und darüber zugreifen |

### Cross-BC-Kommunikation

| Muster | Wann |
|---|---|
| Direkter Service-Call | Synchron, sofortige Antwort nötig |
| Spring Event | Asynchron, Fire-and-Forget |

---

## 8. Wann eskalieren?

| Signal | Schwelle | Aktion |
|---|---|---|
| Klassen in Use-Case-Package | Mapper/Validator oder ≥3 | Sub-Package für Endpunkt |
| Use Cases pro BC | >25–30 | Ressourcen-Gruppierung |
| Klassen pro BC | >60–80 | Sub-BC erwägen |
| Aggregate pro BC | >12–15 | Sub-BC erwägen |
| ArchUnit-Zyklen | Jeder | Sofort auflösen |

Neue Ebene folgt immer dem Grundmuster.

---

## 9. CI-Verifikation

```java
@Test
void verifyModulithStructure() {
    ApplicationModules.of(Application.class).verify();
}
```

### ArchUnit-Regeln (Minimum)

1. Kein Import von `config.*` in fachlichem Code
2. Kein Zugriff auf `otherBc.internal.*`
3. Kein `@Transactional` außerhalb von Use-Case-Klassen
4. Keine direkte Referenz zwischen Use Cases

---

## Spickzettel

```
config/       → Framework. Nur Top-Level. Nie importieren.
common/       → Geteilter Code. Jede Ebene. Nach unten sichtbar.
{bc}/         → Bounded Context. Public API = Service + Events.
  common/     → BC-intern geteilt. domain/, error/, persistence/.
  {usecase}/  → Ein Endpunkt = eine Klasse. Kein Service.
    Klasse    → Aktionsname. Request/Response als Inner Records.
    Entity    → JPA. Postfix "Entity". Domänenklasse ohne Postfix.
```
