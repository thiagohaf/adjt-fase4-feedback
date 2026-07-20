# Adversarial Architecture Review — ARCHITECTURE-SPINE.md

**Reviewer role:** Adversarial architecture reviewer (Reviewer Gate)  
**Artifact:** `ARCHITECTURE-SPINE.md` (draft, 2026-07-20)  
**Method:** For each finding, construct two implementation units one level below the spine (epic teams / modules) that **each satisfy every cited AD literally** yet **integrate incompatibly** — clashing data shapes, dual ownership, or divergent mutation/read paths. Each pair is a hole to close with a new or tightened AD.

---

## Verdict

**CONDITIONAL PASS — substrate is directionally sound but not yet build-safe.**

The spine prevents the obvious failures (microservice sprawl, Lambda-as-API, alert-before-persist, MSK in prod). It does **not** yet prevent **lawful divergence** between the monolith API module and the two Lambda modules, nor between local Kafka and prod SQS adapters. A solo developer can still ship three green CI pipelines that disagree on schema, event JSON, admin e-mail binding, report time windows, and HTTP error codes — all while citing AD-1 through AD-14.

**Minimum to unblock implementation:** add 5–7 tightened ADs (or promote conventions to ADs) covering read-model contract for `lambda-report`, shared alert payload DTO, enrollment/evaluation DB uniqueness, admin e-mail secret binding, report window semantics, and error envelope codes.

---

## Clash Pairs (AD-compliant, integration-incompatible)

### CP-1 — Epic **API (catalog + enrollment + evaluation)** vs Epic **`lambda-report`**

| | Team API | Team Report |
|---|---|---|
| **Scope** | `apps/api` — Flyway, JPA entities, write path | `lambdas/report` — Spring Boot + SCF, read/aggregate |
| **AD compliance** | AD-3 sole writer; AD-2 layering; AD-4 persist + urgency in TX | AD-3 read-only; AD-6 single Lambda, `periodo`; AD-13 SCF stack |
| **Lawful choices** | Migration `V1`: table `avaliacao`, columns `created_at`, `nota`, `urgencia`, FK `aula_id` | Own `@Entity AvaliacaoReport` in lambda module; query `WHERE created_at BETWEEN :start AND :end` |
| **Clash** | Domain/event field in AD-5 is `ocorridoEm`; API never creates that column | Report filters `created_at` (UTC instants); daily bucket uses `[now-24h, now)` |
| **Symptom** | API returns 201 with correct evaluation | Report e-mail/PDF show **zero** evaluations or wrong day counts; demo scene 9–10 fail |
| **Root hole** | AD-3 forbids Lambda **writes** but does not assign **read contract** ownership; Structural Seed puts Flyway only under `api/` while `lambda-report` is a separate deployable with no schema coupling rule |

**Proposed AD-15 — Read model & schema ownership**

- **Binds:** `lambda-report`, Flyway, RDS
- **Rule:** All DDL lives in `apps/api/.../db/migration/` (sole Flyway owner). `lambda-report` **must not** ship migrations or mutate schema. Reads use either (a) a shared read-only module `libs/persistence-readmodel` with entities/DTOs versioned with API migrations, or (b) documented SQL views (`v_avaliacao_relatorio`) created by API migrations. Canonical instant column for report windows: **`ocorrido_em TIMESTAMPTZ NOT NULL`** (persisted at evaluation create = event `ocorridoEm`). Lambda **must not** use `created_at` unless aliased to the same column in the view.

---

### CP-2 — Module **`infrastructure` (Kafka adapter)** vs Module **`infrastructure` (SQS adapter)**

| | Team Local (Kafka) | Team AWS (SQS) |
|---|---|---|
| **Scope** | `EvaluationEventPublisher` → Kafka adapter, profile `local` | Same port → SQS adapter, profile `aws` |
| **AD compliance** | AD-7 Kafka Compose-only; port abstracts broker; domain has no Kafka import | AD-7 SQS in AWS; AD-5 self-contained SQS body |
| **Lawful choices** | Topic `feedbacks.evaluation.high`, value = CloudEvents wrapper `{ "specversion","type","data":{...} }` | Queue message body = flat JSON `{ avaliacaoId, descricao, ... }` per AD-5 |
| **Clash** | `lambda-notification` tested locally against Kafka consumer stub parsing CloudEvents | Production SQS delivers flat JSON; **or** notification Lambda expects wrapper and drops messages |
| **Symptom** | Local demo works; AWS alert never sends despite 201 on API | Inverse: AWS works after hotfix; local profile silently diverges |
| **Root hole** | AD-5 defines **SQS** contract only; AD-7 says “adapters per environment” but not **payload parity** |

**Proposed AD-7a — Alert payload parity (Kafka ≡ SQS body)**

- **Binds:** AD-5, AD-7, `EvaluationEventPublisher`, both adapters
- **Rule:** Extract **`HighUrgencyAlertMessage`** (Java record) in `libs/messaging-contract` (or `domain` event package). Kafka adapter publishes **the identical JSON serialization** as SQS message body (topic/queue is transport only; no CloudEvents/envelope wrapper in MVP). Field names and types fixed: AD-5 list + ISO-8601 offset datetime for `ocorridoEm`. Contract tested by shared JSON fixture consumed in unit tests of both adapters and `lambda-notification`.

---

### CP-3 — Submodule **Enrollment** vs Submodule **Evaluation** (both inside API)

| | Team Enrollment | Team Evaluation |
|---|---|---|
| **Scope** | FR-5 — `InscricaoCurso`, `InscricaoAula` use cases | FR-6/FR-7 — create evaluation |
| **AD compliance** | AD-3 API owns writes; AD-2 use cases + ports | AD-4 “validar inscrição + unicidade Estudante+Aula” |
| **Lawful choices** | Single table `inscricao` with `(usuario_id, aula_id)` unique; optional `curso_id` denormalized | Pre-check: `existsByUsuarioAndAula` only |
| **Clash** | Student enrolls **only** in Aula (shortcut endpoint) | Evaluation allowed without Curso enrollment |
| **Symptom** | FR-5 consequence “inscrição em Aula de Curso no qual o Estudante não está inscrito é rejeitada” **never enforced**; or duplicate Curso enrollment returns 200 idempotent while Evaluation expects 409 |
| **Root hole** | ER diagram shows **both** `INSCRICAO_CURSO` and `INSCRICAO_AULA`; AD-4 mentions “validar inscrição” once without **two-table rule** or unique scopes; Consistency table says 409 for duplicate inscrição but not **which** inscrição |

**Proposed AD-16 — Enrollment invariants**

- **Binds:** FR-5, FR-6, AD-4
- **Rule:** Two persisted aggregates/tables: `inscricao_curso(usuario_id, curso_id)` and `inscricao_aula(usuario_id, aula_id)`. DB **`UNIQUE`** on each pair. Creating `inscricao_aula` **requires** existing `inscricao_curso` for the Aula’s Curso. Duplicate on either → HTTP **409** code **`INSCRICAO_DUPLICADA`**. No idempotent success on duplicate in MVP.

---

### CP-4 — **`lambda-notification`** vs **`lambda-report`** (admin e-mail config)

| | Team Notification | Team Report |
|---|---|---|
| **Scope** | SQS → SES alert | EventBridge → aggregate → SES HTML + PDF |
| **AD compliance** | AD-5 no DB; AD-12 secrets; AD-13 SCF | AD-6 report delivery; AD-12 secrets; AD-13 SCF |
| **Lawful choices** | `@Value("${notification.admin-email}")` from env `ADMIN_EMAIL` | Read Secrets Manager key `ses/toAddress` via Spring Cloud AWS |
| **Clash** | CDK passes secret `feedbacks/config` JSON key `adminEmail` to notification only | Report Lambda expects plain env `SES_RECIPIENT`; not provisioned |
| **Symptom** | Alerts reach admin; report e-mails go to `null` or default sandbox sender loop; or opposite |
| **Root hole** | AD-12 lists “e-mail admin” in SM but **no key name, no binding rule for both Lambdas**, no relation to SES verified identity |

**Proposed AD-12a — Admin notification e-mail binding**

- **Binds:** `lambda-notification`, `lambda-report`, CDK, FR-10, FR-12
- **Rule:** Single Secrets Manager secret (e.g. `feedbacks/app-config`) with key **`adminNotificationEmail`**. **Both** Lambdas receive the same ARN + key via env (`ADMIN_NOTIFICATION_EMAIL` resolved at boot). Value must match SES verified identity. CDK unit test or synth assertion: both functions reference identical secret/key. API does **not** send user-facing mail in MVP — no third binding path.

---

### CP-5 — Layer **`api/web`** vs Layer **`application/domain`** (error envelope)

| | Team Web (controllers + `@ControllerAdvice`) | Team Application (use cases) |
|---|---|---|
| **Scope** | HTTP mapping | Business exceptions |
| **AD compliance** | AD-9 `/api/v1/` | Consistency: `{ code, message, traceId }` |
| **Lawful choices** | Map all `409` → `{ "code":"CONFLICT", ... }` | Throw `AvaliacaoDuplicadaException`, `InscricaoDuplicadaException` with distinct codes |
| **Clash** | Postman collection asserts `code == AVALIACAO_DUPLICADA` | Handler collapses codes |
| **Symptom** | Functionally correct status codes; **contract tests** and demo script disagree; 422 validation returns Spring default `{timestamp, errors[]}` |
| **Root hole** | Error shape lives in **Consistency Conventions** table, not an AD; no stable **`code` enum**; no rule for validation vs conflict vs auth |

**Proposed AD-17 — HTTP error envelope**

- **Binds:** all API HTTP errors, AD-9
- **Rule:** All non-2xx responses use **`{ "code", "message", "traceId" }`** (no Spring default body). `traceId` = incoming `X-Request-Id` or generated UUID stored in MDC. Minimum stable codes: `VALIDATION_ERROR` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `INSCRICAO_DUPLICADA` (409), `AVALIACAO_DUPLICADA` (409). Validation may set `message` to first field error; optional `details` array allowed but `code` required.

---

### CP-6 — **`lambda-report`** (window logic) vs **Infra/CDK** (EventBridge cron)

| | Team CDK | Team Report aggregation |
|---|---|---|
| **Scope** | Two rules, timezone `America/Sao_Paulo` | SQL/Java window for `periodo=diario\|semanal` |
| **AD compliance** | AD-6 “duas regras EventBridge (`America/Sao_Paulo`)" | AD-6 parameter `periodo`; Consistency “janelas … dia civil” |
| **Lawful choices** | Daily `cron(0 8 * * ? *)`, weekly Monday 08:00 SP | Daily window: UTC `[triggerInstant - 24h, triggerInstant)`; weekly: `[triggerInstant - 7d, triggerInstant)` |
| **Clash** | Cron fires 08:00 **Monday** SP | “7 dias civis anteriores” interpreted as rolling 168h UTC → **includes/excludes wrong calendar days**; evaluations near midnight SP land in wrong bucket |
| **Symptom** | FR-11 “qty por dia” table shows misaligned dates; Sunday 23:00 SP evaluations missing from Monday daily report |
| **Root hole** | AD-6 specifies cron TZ but **not** inclusive bounds, anchor field, or “civil day” algorithm in Lambda |

**Proposed AD-6a — Report window semantics**

- **Binds:** FR-11, FR-17, AD-6, `lambda-report`
- **Rule:** Windows computed in **`America/Sao_Paulo`**:  
  - **Diário:** `[startOfDaySP(yesterday), endOfDaySP(yesterday)]` inclusive.  
  - **Semanal:** seven consecutive **calendar** days ending yesterday SP (Mon–Sun table in PDF).  
  Filter column: **`ocorrido_em`** only. Store/compare as `timestamptz`. Manual invoke (AD-10) uses same algorithm with `triggerInstant = now()` SP. EventBridge cron remains 08:00 SP daily / Monday weekly. Document boundary examples in `docs/report-windows.md`.

---

### CP-7 — **Evaluation uniqueness** (application check vs DB vs report aggregation)

| | Team Application | Team DBA / Report |
|---|---|---|
| **Scope** | AD-4 transactional create | Report SQL `COUNT`, `AVG` |
| **AD compliance** | AD-4 “unicidade Estudante+Aula”; Consistency 409 | AD-3 read-only; AD-6 aggregate |
| **Lawful choices** | Check-then-insert in Java (no DB unique index) | Report joins `avaliacao` without dedup |
| **Clash** | Race: two POSTs → two rows | Report **double-counts** averages and urgency buckets; API might return 409 on one and 201 on both under load |
| **Symptom** | Violates FR-7 “no máximo uma Avaliação por par Estudante+Aula” intermittently |
| **Root hole** | AD-4 states rule in prose; no **mandatory UNIQUE constraint** or conflict mapping |

**Proposed AD-4a — Evaluation uniqueness enforcement**

- **Binds:** FR-7, AD-4
- **Rule:** DB **`UNIQUE (estudante_id, aula_id)`** on `avaliacao`. Application may pre-check, but constraint is source of truth. Violation → **409 `AVALIACAO_DUPLICADA`**. No soft-delete/resubmit pattern in MVP.

---

### CP-8 — **Domain urgency** vs **`lambda-report` aggregation** vs **AD-5 event**

| | Team Domain | Team Report |
|---|---|---|
| **AD compliance** | Consistency: urgency enum `ALTA\|MEDIA\|BAIXA` derived in domain | AD-6 aggregate by urgency level |
| **Lawful choices** | Persist enum as Java `ALTA` | SQL `GROUP BY nota` re-deriving buckets with `CASE WHEN nota <= 4` |
| **Clash** | If nota stored as decimal or urgency manually patched in seed | Report counts disagree with API list and with stored `urgencia`; alert count ≠ report ALTA count |
| **Root hole** | Urgency derivation stated in conventions, not bound to **read path** for reports |

**Proposed AD-9a — Urgency as persisted source of truth**

- **Binds:** FR-9, FR-11, FR-17, AD-4, AD-5
- **Rule:** Reports **must aggregate on persisted `urgencia`**, never re-derive from `nota`. Boundaries ≤4 / 5–7 / ≥8 tested at domain unit tests; DB check constraint optional. AD-5 event carries same enum string values.

---

## Focused Audit (requested checks)

### 1. Lambda report reading RDS vs API ownership

| Check | Status | Notes |
|---|---|---|
| Write ownership | ✅ | AD-3 clear: API sole writer |
| Read ownership / schema | ❌ **Gap** | CP-1: Lambda may invent parallel entity model |
| Migration ownership | ⚠️ | Flyway only under `api/` in Structural Seed — implicit but not an AD |
| Coupling on schema change | ❌ | API can rename column; report breaks silently |

**Recommendation:** AD-15 (above).

---

### 2. Kafka vs SQS adapter contracts

| Check | Status | Notes |
|---|---|---|
| Port abstraction | ✅ | AD-7 `EvaluationEventPublisher` |
| Payload equivalence local/prod | ❌ **Gap** | CP-2 |
| Shared test fixture | ❌ | Not mentioned |
| Domain isolation from clients | ✅ | AD-7 prevents domain importing clients |

**Recommendation:** AD-7a + shared `libs/messaging-contract`.

---

### 3. Enrollment uniqueness

| Check | Status | Notes |
|---|---|---|
| Duplicate rejection | ⚠️ | Consistency table + FR-5 assumption; not an AD |
| Curso + Aula both required | ⚠️ | PRD/ER yes; AD-4 only says “validar inscrição” |
| Which unique keys | ❌ **Gap** | CP-3 |
| HTTP code | ⚠️ | 409 mentioned generically, not `INSCRICAO_DUPLICADA` |

**Recommendation:** AD-16.

---

### 4. Evaluation uniqueness

| Check | Status | Notes |
|---|---|---|
| Business rule stated | ✅ | AD-4 + FR-7 |
| DB enforcement | ❌ **Gap** | CP-7 |
| Estudante identity | ⚠️ | Implicit JWT subject; not stated in AD-4 |
| Re-evaluation / upsert | ✅ | PRD closed: no upsert |

**Recommendation:** AD-4a + explicit “estudante_id = JWT sub” in AD-4.

---

### 5. Admin e-mail config

| Check | Status | Notes |
|---|---|---|
| Secret store | ✅ | AD-12 |
| Key name & Lambda binding | ❌ **Gap** | CP-4 |
| SES verified identity alignment | ⚠️ | PRD assumption only |
| API involvement | ✅ | Correctly absent for mail send |

**Recommendation:** AD-12a.

---

### 6. Error envelope

| Check | Status | Notes |
|---|---|---|
| Shape documented | ⚠️ | Consistency table only — not binding AD |
| Stable machine codes | ❌ **Gap** | CP-5 |
| Validation errors | ❌ | Not specified |
| Lambda errors | N/A | Non-HTTP; CloudWatch logs only — acceptable if documented |

**Recommendation:** AD-17.

---

### 7. Timezone for reports

| Check | Status | Notes |
|---|---|---|
| Cron TZ | ✅ | AD-6 EventBridge `America/Sao_Paulo` |
| Window algorithm in Lambda | ❌ **Gap** | CP-6 |
| Column for filtering | ❌ | `ocorridoEm` in AD-5 vs no DB column AD |
| Weekly vs daily coexistence | ✅ | AD-6 + FR-17 |
| Manual invoke (AD-10) same semantics | ⚠️ | Not stated — manual could use “last 24h from invoke” vs “yesterday SP” |

**Recommendation:** AD-6a + AD-10 cross-ref “manual invoke uses AD-6a windows”.

---

## Additional Holes (no single team pair, still adversarial)

1. **AD-5 missing estudante identity in alert payload** — FR-10 requires description, urgency, send date; admin may not need student id, but email template teams may add fields not in contract → notification Lambda strict parsing fails. *Tighten AD-5: closed field list, no extras required for parse.*

2. **S3 key convention vs period label** — Consistency: `relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf`. Team A uses `periodo=diario`; Team B uses `daily`. Both satisfy “distinguishable by path” literally; demo script breaks. *Tighten: `periodo` enum `diario|semanal` in path segment.*

3. **JWT claim `role` vs Spring authority** — AD-8 + Consistency `role` claim; Team Security uses `ROLE_ADMINISTRADOR` vs claim `ADMINISTRADOR`. AD-compliant either way; authorization matrix diverges. *Tighten AD-8: claim value = authority suffix without `ROLE_` prefix mapping rule.*

4. **Publish-after-commit mechanism unspecified** — AD-4 requires after-commit publish; Team A uses `@TransactionalEventListener(phase = AFTER_COMMIT)`; Team B uses fire-and-forget in controller after service returns (before commit). Both “intended” to follow AD-4. *Tighten AD-4: publish hook must be AFTER_COMMIT; forbid controller-level publish.*

5. **Shared DTO module vs duplicate records** — Structural Seed has no `libs/`; AD-13 Lambdas are separate Spring Boot apps. Nothing forces shared AD-5 DTO between API publisher and notification consumer. *Add Structural Seed `libs/messaging-contract` or bind AD-7a module path.*

6. **ER diagram `USUARIO ||--o| AVALIACAO`** — cardinality reads one evaluation per user globally, contradicting FR-7 (per Aula). Documentation clash, not AD — confuses implementers. *Fix diagram to `||--o{`.*

7. **JaCoCo 90% (AD-14) per module without contract tests** — Teams can hit 90% with mocked wrong JSON shapes; integration mismatch undetected. *Add note: at least one adapter contract test is part of Definition of Done for AD-7a.*

8. **Actuator health path vs AD-9 public health** — AD-9 “login e health públicos”; Team A exposes `/actuator/health`; Team B custom `/api/v1/health`. Both public; Postman collection diverges. *Tighten AD-9: canonical public path `/actuator/health` (or single documented alias).*

---

## Summary Table — Holes → Proposed AD Actions

| ID | Hole | Proposed action |
|---|---|---|
| H-1 | Lambda report read model decoupled from API schema | **AD-15** Read model ownership |
| H-2 | Kafka body ≠ SQS body | **AD-7a** Payload parity + shared module |
| H-3 | Enrollment two-table rules unclear | **AD-16** Enrollment invariants |
| H-4 | Evaluation uniqueness not DB-backed | **AD-4a** UNIQUE + 409 code |
| H-5 | Admin e-mail secret not bound to both Lambdas | **AD-12a** Single key binding |
| H-6 | Report civil-day window unspecified | **AD-6a** Window semantics |
| H-7 | Error envelope not binding | **AD-17** Stable codes |
| H-8 | Urgency re-derived in reports | **AD-9a** Persisted urgency for aggregates |
| H-9 | After-commit publish mechanism | Tighten **AD-4** |
| H-10 | S3 path period segment | Tighten Consistency or **AD-6** |

---

## Recommended Next Step

Apply the proposed AD-15–AD-17 (and sub-clauses 4a, 6a, 7a, 9a, 12a) to the spine **before** epic/story split. Add `libs/messaging-contract` (and optionally `libs/persistence-readmodel`) to Structural Seed. Re-run this review after patch — expect PASS when every CP above has a binding AD and a testable acceptance check.

---

*Review completed: 2026-07-20 — adversarial gate for `architecture-adjt-fase4-2026-07-20`.*
