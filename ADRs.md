# Architecture Decision Records – Spring Boot Projektstruktur

---

## ADR-001: Rekursives Strukturprinzip

**Status:** Accepted

**Kontext:** Spring Boot bietet keine offizielle Guidance für Package-Strukturen jenseits von Package-by-Layer. Bestehende Ansätze (Package-by-Feature, Hexagonal, Modulith) lösen jeweils Teilprobleme, keiner adressiert alle Ebenen konsistent.

**Entscheidung:** Jede Hierarchieebene folgt demselben Schema: `config/` + `common/` + `{fachliche-packages}/`. Das Muster ist rekursiv anwendbar von der App-Ebene bis zum Use Case.

**Begründung:**
- Einheitliches mentales Modell auf jeder Ebene.
- Neue Entwickler müssen nur ein Muster lernen.
- Skaliert von 3 Klassen bis zu großen Modulithen.
- Kein konkurrierender Ansatz bietet diese Konsistenz.

**Verworfene Alternativen:**
- Package-by-Layer: Keine Kohäsion, nicht extrahierbar.
- Hexagonal/Clean Architecture: Mapping-Overhead, ~8–12 Klassen pro Feature, Framework-Wechsel in der Praxis irrelevant.
- Reines Package-by-Feature: Keine Antwort auf Cross-Cutting Concerns, keine Binnenstruktur.

---

## ADR-002: `config/` nur auf Top-Level

**Status:** Accepted

**Kontext:** Framework-Konfiguration (Security, Web, Persistence, Error-Handling) muss irgendwo leben. Dezentrale Konfiguration pro BC ist schwer überblickbar.

**Entscheidung:** `config/` existiert ausschließlich auf Top-Level. Bounded Contexts konfigurieren das Framework nicht.

**Begründung:**
- Eine Stelle für alle Framework-Einstellungen.
- Security-Lücken durch vergessene BC-spezifische Konfiguration ausgeschlossen.
- `GlobalExceptionHandler` ist Framework-Konfiguration, keine Fachlogik.
- Klare ArchUnit-Regel möglich: kein Import von `config.*` in fachlichem Code.

**Verworfene Alternativen:**
- Dezentrale Security pro BC: Gefahr von Lücken, schwer zu auditieren.
- Hybrid (zentrale Basis + BC-Erweiterungen): Komplex, schwer nachvollziehbar.

---

## ADR-003: Zwei-Package-Modell für Cross-Cutting Concerns (`config` + `common`)

**Status:** Accepted

**Kontext:** Die bestehende Literatur versagt bei der Frage, wohin Non-Domain-Code gehört. Übliche Ansätze (`_infrastructure`, `shared`, `support`) vermischen Framework-Setup mit fachlich geteiltem Code.

**Entscheidung:** Zwei getrennte Packages:
- `config/` für Framework-Konfiguration (nur vom Framework genutzt).
- `common/` für geteilten fachlichen Code (von BCs genutzt).

**Begründung:**
- Framework-Setup und fachliche Value Objects haben unterschiedliche Konsumenten.
- `config` wird nur vom Framework gelesen, nie von fachlichem Code referenziert.
- `common` enthält Code, den BCs aktiv importieren (Money, Address, BaseEntity).
- Klare Zugriffsregel: fachlicher Code → `common` ja, `config` nein.

**Verworfene Alternativen:**
- Monolithisches `infrastructure`-Package: Vermischt zwei Kategorien.
- Alles in `common`: Framework-Config wird ungewollt fachlich referenzierbar.

---

## ADR-004: `common/` auf jeder Hierarchieebene

**Status:** Accepted

**Kontext:** Geteilter Code existiert nicht nur app-weit. Auch innerhalb eines BC teilen mehrere Use Cases Entities, Repositories und Error-Enums.

**Entscheidung:** `common/` darf auf jeder Ebene existieren: App-weit, BC-weit, ggf. Sub-BC-weit. Zugriff nur von der eigenen Ebene und darunter.

**Litmustest:**
- \>50% der Module nutzen es → `common` (App-Ebene)
- 2–3 Module → `common` der übergeordneten Ebene
- 1 Modul → gehört IN dieses Modul
- Eigener Lifecycle → eigenes Modul

**Begründung:**
- Verhindert, dass geteilter BC-interner Code künstlich nach oben wandert.
- Rekursive Konsistenz mit dem Grundprinzip (ADR-001).

---

## ADR-005: Package-Namen ohne Underscore

**Status:** Accepted

**Kontext:** Underscores (`_config`, `_common`) könnten die Package-Sortierung in IDEs verbessern und Cross-Cutting-Packages visuell abtrennen.

**Entscheidung:** Keine Underscores. `config` und `common` als reservierte Konventionsnamen.

**Begründung:**
- JLS erlaubt Underscores, aber Google Java Style Guide und SonarLint (squid:S00120) verbieten sie.
- Spring, Apache, Jackson nutzen nirgends Underscores.
- `c` sortiert in den meisten Fällen ohnehin vor den Domain-Packages.
- Konvention „`config` und `common` sind reserviert" ist ausreichend.

**Kompromiss:** Generator bietet optionalen Underscore-Switch (Default: off).

---

## ADR-006: Single-Action-Controller als Default

**Status:** Accepted

**Kontext:** Die meisten Spring-Projekte folgen dem Pattern Controller → Service → Repository. Häufig ist der Service ein reiner Pass-Through, der nur von einem Controller gerufen wird.

**Entscheidung:** Default ist ein Controller pro Endpunkt mit Logik direkt in der Klasse. Kein separater Service. Request/Response als `static` Inner Records.

**Begründung:**
- Eliminiert die überflüssige Service-Schicht bei 1:1-Beziehung.
- Jede Klasse ist self-contained: Endpunkt, DTOs, Logik, Abhängigkeiten.
- Maximale Kohäsion: Alles, was zu einem Endpunkt gehört, an einer Stelle.
- YAGNI: Service erst extrahieren, wenn mehrere Aufrufer existieren.

**Wann Service extrahieren:**
- Zweiter Controller (z.B. Admin-API) braucht dieselbe Logik.
- Event-Listener, Scheduled Job oder anderer Use Case ruft dieselbe Logik auf.

**Verworfene Alternativen:**
- Thin Controller + Application Service als Default: Erzeugt systematisch Pass-Through-Klassen.
- Multi-Method-Controller (alle Endpunkte einer Ressource): Wird bei komplexen BCs zur God Class.

**Review-Ergebnis (Kritik v1.0):** Vorschlag, Single-Action-Controller optional per Flag zu machen, abgelehnt. Das Problem "zu viel Logik im Controller" wird durch Domänenlogik im Domänenobjekt gelöst (ADR-007), nicht durch eine Service-Schicht.

---

## ADR-007: Domänenlogik gehört ins Domänenobjekt

**Status:** Accepted

**Kontext:** Kritik an ADR-006 argumentiert, dass Controller bei >5–8 Zeilen unübersichtlich werden und Unit-Tests ohne Spring-Context schwierig seien.

**Entscheidung:** Komplexe Geschäftslogik gehört ins Domänenobjekt (Entity, Value Object, Domain Class), nicht in den Controller und nicht in einen Service.

**Begründung:**
- Der Controller bleibt dünn: Request annehmen → Domänenobjekt aufrufen → Repository aufrufen → Response zurückgeben.
- Domänenobjekte sind trivial unit-testbar ohne Spring-Context.
- Anemic Domain Model (Logik in Services statt Entities) ist ein bekanntes Anti-Pattern.
- Wenn der Controller trotzdem zu groß wird, greift die Action-Package-Eskalation (ADR-009).

**Beispiel:**
```java
// Logik im Domänenobjekt, nicht im Controller
var draft = PolicyDraft.create(tsid.next(), req.holderName(), req.coverage());
draft.validate(); // Domänenobjekt validiert sich selbst
repo.save(draft);
```

---

## ADR-008: Naming-Konventionen

**Status:** Accepted

**Kontext:** Klassen-Postfixe signalisieren technische Rollen. Die Frage ist, welche Postfixe gebraucht werden und welche vermieden werden sollten.

**Entscheidung:**

| Postfix | Verwendung | Beispiel |
|---|---|---|
| *(keiner)* | Domänenklasse, DTO, Value Object | `PolicyDraft`, `Money` |
| *(keiner)* | Endpunkt-Klasse (Aktionsname) | `CreatePolicyDraft` |
| `Entity` | JPA-annotierte Persistenzklasse | `PolicyDraftEntity` |
| `Repository` | Spring Data Interface | `PolicyDraftRepository` |
| `Service` | Geteilte Geschäftslogik / BC Public API | `DesignerService` |
| `Error` | Guard4j Error-Enum | `DesignerError` |

Kein `Controller`-Postfix. Kein `Dto`-Postfix.

**Begründung:**
- Fachliche Namen bleiben sauber: `PolicyDraft` statt `PolicyDraftDto`.
- `Entity`-Postfix trennt JPA-Klasse von Domänenklasse. Ermöglicht JPA-Projektionen direkt in DTOs.
- Aktionsname als Klassenname (`CreatePolicyDraft`) ist selbsterklärend. `@RestController` signalisiert die Rolle.
- `Controller`-Postfix wäre bei Single-Action-Controllern irreführend (suggeriert Multi-Method).

**Review-Ergebnis (Kritik v1.0):** Vorschlag für optionalen `Dto`-Suffix abgelehnt. Namenskollisionen werden durch den `Entity`-Postfix verhindert, nicht durch `Dto`.

---

## ADR-009: Action-Package-Eskalation

**Status:** Accepted (Schwelle angepasst nach Review)

**Kontext:** Single-Action-Controller (ADR-006) erzeugen flache Use-Case-Packages. Wenn ein Endpunkt zusätzliche Klassen braucht (Mapper, Validator, Strategy), wird das Package unübersichtlich.

**Entscheidung:** Ab einem Mapper, einem Validator oder ≥3 Klassen insgesamt → eigenes Sub-Package für den Endpunkt.

```
creation/
├── CreatePolicyDraft.java           ← einfach, bleibt flach
├── GetPolicyDraft.java
├── submitpolicydraft/               ← komplex, eskaliert
│   ├── SubmitPolicyDraft.java
│   ├── SubmitValidator.java
│   └── UnderwritingResult.java
├── PolicyDraft.java
└── PolicyDraftRepository.java
```

**Begründung:**
- Flache Struktur als Default minimiert Overhead.
- Eskalation gruppiert zusammengehörige Klassen visuell.
- Schwelle „≥3 Klassen oder Mapper/Validator" ist praxisnah (Review-Ergebnis).

**Ursprüngliche Schwelle:** >1 zusätzliche Klasse. Nach Review gesenkt auf Mapper/Validator oder ≥3 Klassen.

**Skalierung bei großen BCs (>25 Use Cases):** Wenn ein BC >25–30 Use Cases enthält, darf eine Ressourcen-Gruppierung als Zwischenebene eingeführt werden:

```
policy/
├── common/
├── drafts/
│   ├── CreatePolicyDraft.java
│   ├── GetPolicyDraft.java
│   └── SubmitPolicyDraft.java
├── underwriting/
│   ├── StartUnderwriting.java
│   └── CompleteUnderwriting.java
└── renewal/
    └── RenewPolicy.java
```

Die Zwischenebene folgt dem rekursiven Prinzip (ADR-001) und darf ein eigenes `common/` haben. Diese Eskalation greift nicht automatisch – sie ist eine bewusste Entscheidung bei organischem Wachstum.

**Verworfene Alternativen:**
- `application/` + `domain/` Sub-Packages pro Use Case: Mini-Hexagonal innerhalb jedes Use Cases. Overengineering. Wenn ein Use Case so komplex ist, sollte er aufgeteilt werden.

---

## ADR-010: `{Bc}Service` als reine Fassade

**Status:** Accepted

**Kontext:** Jeder BC braucht eine Public API für Cross-BC-Kommunikation. Die Frage ist, ob diese API-Klasse auch Domänenlogik enthalten darf.

**Entscheidung:** `{Bc}Service` enthält keine Geschäftslogik. Er orchestriert Use Cases und dient als Fassade für andere BCs.

**Begründung:**
- Verhindert, dass `{Bc}Service` zur God Class wird.
- Domänenlogik, die von mehreren Use Cases gebraucht wird, gehört in Hilfsklassen unter `{bc}/common/domain/` (z.B. `PolicyNumberGenerator.java`).
- Klare Verantwortlichkeit: `{Bc}Service` = Routing + Delegation, nicht Berechnung.

**Review-Ergebnis (Kritik v1.0):** Vorschlag, "kleine invariant-protecting Methoden" im Service zu erlauben, abgelehnt. `ensurePolicyIsDraft()` gehört auf das Domänenobjekt. `calculateNextPolicyNumber()` gehört in `{bc}/common/domain/`.

---

## ADR-011: Sichtbarkeitsregeln

**Status:** Accepted (Klarstellung nach Review)

**Kontext:** Java-Package-Visibility ist das stärkste Werkzeug zur Kapselung in Spring Boot. Die Frage ist, was `public` sein darf.

**Entscheidung:**

**Public (für Cross-BC-Zugriff):**
- `{Bc}Service`
- Events

**Public (technisch notwendig, aber nicht für Cross-BC-Zugriff):**
- Use-Case-Klassen (`@RestController` erfordert `public`)
- Entities, Repositories (Spring Data erfordert `public`)

**Regel:** Klassen sind `public` wo Spring es erfordert, aber kein Use Case injiziert einen anderen Use Case. Cross-BC-Zugriff nur über `{Bc}Service` und Events.

**Klarstellung nach Review:** Die ursprüngliche Formulierung "Keine public Use Cases" war missverständlich. Spring-Beans müssen `public` sein. Die Regel betrifft den *Zugriff*, nicht die *Sichtbarkeit*: Kein Code außerhalb des Use-Case-Packages darf einen Use Case direkt referenzieren.

---

## ADR-012: Transaktionen auf Use-Case-Ebene

**Status:** Accepted

**Kontext:** `@Transactional` kann auf Service-, Controller- oder Repository-Ebene platziert werden.

**Entscheidung:** `@Transactional` gehört auf die Use-Case-Klasse (den Single-Action-Controller oder, bei extrahiertem Service, auf den Service).

| Typ | Annotation |
|---|---|
| Schreibender Use Case | `@Transactional` |
| Lesender Use Case | `@Transactional(readOnly = true)` |
| Event Listener (schreibend) | `@Transactional` |
| `{Bc}Service` | keine (delegiert nur) |

**Begründung:**
- Ein Use Case = eine fachliche Operation = eine Transaktion.
- Auf Repository-Ebene: zu granular, keine übergreifende Konsistenz.
- Auf `{Bc}Service`-Ebene: Service enthält keine Logik (ADR-010), also keine Transaktion.

**Escape-Hatches:** In begründeten Sonderfällen (Batch-Verarbeitung, Outbox-Pattern, SAGA-Orchestratoren) ist `@Transactional` auf privaten Methoden oder dedizierten Orchestrator-Klassen erlaubt. Solche Ausnahmen müssen dokumentiert werden (ArchUnit-Ausnahmeregel oder expliziter Kommentar) und in Code-Reviews bestätigt werden. Spring Batch regelt Transaktionsgrenzen eigenständig über Chunk-Processing – dort greift diese ADR nicht.

---

## ADR-013: Cross-BC-Kommunikation

**Status:** Accepted

**Kontext:** BCs müssen kommunizieren. Die Frage ist, über welchen Mechanismus.

**Entscheidung:**

| Muster | Wann |
|---|---|
| Direkter Service-Call | Synchron, Aufrufer braucht sofort Antwort |
| Spring Event | Asynchron, Fire-and-Forget |
| Shared Interface in `common` | Wenn mehrere BCs denselben Service aufrufen |

**Begründung:**
- Service-Call für synchrone Abfragen (z.B. Plugin-Registry: "Welche Plugins sind kompatibel?").
- Events für Zustandsänderungen (z.B. "Workflow aktiviert" → andere BCs reagieren).
- Kein dogmatisches "alles über Events" – das erzeugt implizite Abhängigkeiten und erschwert Debugging.

**Stresstest-Ergebnis (Workflow Designer):** Plugin-Capability-Check = direkter Service-Call. Workflow-Aktivierung = Event. Dry-Run-Start = Event/Command.

---

## ADR-014: Repository-Platzierung

**Status:** Accepted

**Kontext:** Repositories werden häufig von mehreren Use Cases gebraucht. Die Frage ist, wo sie liegen.

**Entscheidung:**

| Fall | Ort |
|---|---|
| 1 Use Case nutzt Aggregate | Im Use-Case-Package |
| ≥2 Use Cases nutzen Aggregate | `{bc}/common/persistence/` |
| BC-übergreifend | Verboten. Zugriff über `{Bc}Service`. |

Repositories sind niemals im Top-Level `common`.

**Begründung:**
- Bei 1 Nutzer: Maximale Kohäsion.
- Ab 2 Nutzern: Künstliche Abhängigkeit zwischen Use Cases vermeiden.
- Schwelle 2 (nicht 3): Bei 2 Use Cases und Repository in einem hat der andere eine unnatürliche Abhängigkeit.

**Review-Ergebnis (Kritik v1.0):** Vorschlag, Schwelle auf 3 zu heben, abgelehnt.

---

## ADR-015: Error Handling mit Guard4j

**Status:** Accepted

**Kontext:** Klassisches Exception-Handling erzeugt tiefe Hierarchien: Basis-Exception, Sub-Exceptions pro Fehlerfall, HTTP-Status-Mapping.

**Entscheidung:** Drei Schichten:

| Schicht | Ort | Was |
|---|---|---|
| App-weite Errors | `common/error/` | `AppError.java` – Guard4j Error-Enum |
| BC-spezifische Errors | `{bc}/common/error/` | `{Bc}Error.java` – Guard4j Error-Enum |
| Exception → HTTP | `config/error/` | `GlobalExceptionHandler` – `@ControllerAdvice` |

**Begründung:**
- Ein Error-Enum pro BC statt 15 Exception-Klassen.
- Jeder Enum-Wert definiert Fehlercode, HTTP-Status, Message-Template.
- Separate Exception-Klassen nur bei gezieltem `catch` (z.B. Retry-Logik).
- `GlobalExceptionHandler` ist Framework-Konfiguration (ADR-002), keine Fachlogik.

---

## ADR-016: Listener als Use Case, nicht als eigenes Package

**Status:** Accepted (mit Eskalation für Events)

**Kontext:** Event-Listener reagieren auf Domänen-Events. Manche Teams wollen sie in einem eigenen `listeners/`-Package sammeln.

**Entscheidung:** Listener sind Use Cases und liegen im Use-Case-Package. Kein `{bc}/listeners/`-Package.

**Ergänzung nach Review:** Publizierte Events (die öffentliche API eines BC) liegen im BC-Root. Ab >3 Events dürfen sie in `{bc}/common/events/` gruppiert werden. Listener bleiben Use Cases.

**Begründung:**
- Ein Listener *tut etwas* – er ist eine fachliche Aktion, ausgelöst durch ein Event statt HTTP.
- `listeners/` wäre Package-by-Layer innerhalb des BC.
- Events als Datenklassen gehören zu `common` (geteilte Strukturen), Listener gehören zum Use Case.

---

## ADR-017: Geteilte Domänenlogik in `{bc}/common/domain/`

**Status:** Accepted

**Kontext:** Manche Domänenlogik wird von mehreren Use Cases gebraucht, gehört aber nicht in `{Bc}Service` (ADR-010) und nicht auf ein einzelnes Domänenobjekt.

**Entscheidung:** Geteilte Domänenlogik, die kein Use Case allein besitzt, gehört in Hilfsklassen unter `{bc}/common/domain/`.

**Beispiele:**
- `PolicyNumberGenerator.java`
- `PremiumCalculator.java`
- `WorkflowValidator.java`

**Begründung:**
- Verhindert Duplikation über Use Cases.
- Hält `{Bc}Service` schlank.
- Folgt dem rekursiven `common`-Prinzip (ADR-004).

---

## ADR-018: Keine `application/` + `domain/` Sub-Packages in Use Cases

**Status:** Rejected (Vorschlag aus Review)

**Kontext:** Review schlägt vor, innerhalb jedes Use Case `application/` und `domain/` zu trennen, analog zu Vertical Slice Architecture mit leichter Hexagonal-Schichtung.

**Entscheidung:** Abgelehnt.

**Begründung:**
- Mini-Hexagonal innerhalb jedes Use Case ist das Overengineering, das wir in der Analyse (Teil 1.4) kritisieren.
- Wenn ein Use Case so komplex ist, dass er innere Schichten braucht, sollte er aufgeteilt werden.
- Die Action-Package-Eskalation (ADR-009) deckt den Bedarf ab, ohne ein Schichtenmodell zu erzwingen.

---

## ADR-019: Modulith-Integration

**Status:** Accepted

**Kontext:** Spring Modulith bietet ArchUnit-basierte Verifikation von Modul-Grenzen. Die Frage ist, wie es in die Struktur integriert wird.

**Entscheidung:**
- Top-Level-Packages = Spring Modulith Module (Auto-Detection).
- `common` als erlaubte Abhängigkeit für alle Module konfigurieren.
- `modules.verify()` im CI.

**ArchUnit-Regeln (mindestens):**
1. Kein Zugriff von einem BC auf `otherBc.internal.*`.
2. Kein Import von `config.*` in fachlichem Code.
3. Kein `@Transactional` außerhalb von Use-Case-Klassen.
4. Keine direkte Referenz zwischen Use Cases.

**Begründung:**
- Modulith liefert den Rahmen, unsere Regeln definieren die Binnenstruktur.
- `common` ist kein eigenständiges Modul mit Geschäftslogik, sondern eine Shared Library.
- CI-Verifikation macht die Regeln unumgehbar.

---

## ADR-020: Entity-Postfix für JPA, saubere Namen für Domäne

**Status:** Accepted

**Kontext:** JPA-Entities und Domänenklassen/DTOs repräsentieren oft dasselbe Konzept. Die Frage ist, wie man sie unterscheidet.

**Entscheidung:** `PolicyDraftEntity` für JPA. `PolicyDraft` für Domänenklasse/DTO.

**Begründung:**
- Fachliche Namen bleiben sauber und lesbar.
- `Entity`-Postfix signalisiert: "Diese Klasse hat JPA-Annotationen und Persistence-Lifecycle."
- Moderne JPA-Implementierungen erlauben Projektionen direkt in DTOs/Records – der saubere Name macht das natürlich.
- Kein `Dto`-Postfix nötig: Request/Response als Inner Records im Controller, Domänenklassen ohne Postfix.

---

## ADR-021: Mehrere Aggregate pro Bounded Context erlaubt

**Status:** Accepted

**Kontext:** DDD-Puristen fordern 1 Aggregate = 1 BC. In der Praxis haben die meisten BCs mehrere eng verwandte Aggregate.

**Entscheidung:** Ein BC darf mehrere Aggregate enthalten.

**Begründung:**
- 1:1-Mapping Aggregate-zu-BC erzeugt viele winzige BCs mit hoher Kommunikationslast.
- Stresstest (Workflow Designer): Workflow + Stage + PluginRef sind eng verflochten, aber verschiedene Aggregate.
- Consistency-Grenzen werden durch Transaktionen auf Use-Case-Ebene gewahrt (ADR-012).

---

## ADR-022: BC-Grenze durch Public API

**Status:** Accepted

**Kontext:** Wie wird die Grenze eines Bounded Context technisch durchgesetzt?

**Entscheidung:** Die Public API eines BC besteht ausschließlich aus:
- `{Bc}Service` (synchrone Aufrufe)
- Events (asynchrone Kommunikation)

Kein anderer BC darf auf interne Klassen (Entities, Repositories, Use Cases, Common) zugreifen.

**Begründung:**
- Enge API minimiert Kopplung.
- Extraktion zu Microservice wird möglich: Service → REST-Client, Events → Message Broker.
- Package-Scope + Modulith `verify()` + ArchUnit setzen dies technisch durch.

---

## ADR-023: Hierarchische Eskalation – Wann ein BC zu groß wird

**Status:** Accepted

**Kontext:** Das rekursive Strukturprinzip (ADR-001) definiert keine Obergrenze für die Größe eines BC oder die Anzahl der Hierarchieebenen. In der Praxis wachsen BCs organisch, und es braucht klare Signale, wann eine Aufteilung nötig ist.

**Entscheidung:** Bei Bedarf darf eine zusätzliche Hierarchieebene eingefügt werden. Die neue Ebene muss dem rekursiven Muster folgen (`common/` + fachliche Packages).

**Schwellen (Signale, keine harten Grenzen):**

| Signal | Schwelle | Aktion |
|---|---|---|
| Use Cases pro BC | >25–30 | Ressourcen-Gruppierung als Zwischenebene (ADR-009) |
| Klassen pro BC | >60–80 | Sub-BC erwägen |
| Aggregate pro BC | >12–15 | Sub-BC erwägen |
| ArchUnit-Zyklen | Jeder Zyklus | Sofort auflösen: Klassen verschieben oder BC teilen |
| Merge-Konflikte | Häufig im selben BC | Starkes Signal für Aufteilung |

**Beispiel:** Ein `policy`-BC mit 70+ Klassen wird aufgeteilt:

```
policy/                              ← bleibt BC
├── common/                          ← geteilter Code
├── drafts/                          ← Sub-BC oder Use-Case-Gruppe
│   ├── common/
│   ├── CreatePolicyDraft.java
│   └── ...
├── underwriting/                    ← Sub-BC oder Use-Case-Gruppe
│   ├── common/
│   └── ...
└── renewal/
```

**Begründung:**
- Verhindert, dass ein BC zur God-Module wird.
- Schrittweise Verfeinerung ohne Bruch des mentalen Modells.
- Die Schwellen sind Erfahrungswerte, keine dogmatischen Grenzen. Teams sollen sie als Diskussionsanlass nutzen.

**Abgrenzung:** Die Entscheidung, ob eine neue Ebene ein Sub-BC oder eine reine Use-Case-Gruppierung ist, hängt davon ab, ob die Teile eigene Aggregate besitzen. Eigene Aggregate → Sub-BC mit eigenem `common/`. Nur Use-Case-Gruppierung → kein eigenes `common/` nötig.
