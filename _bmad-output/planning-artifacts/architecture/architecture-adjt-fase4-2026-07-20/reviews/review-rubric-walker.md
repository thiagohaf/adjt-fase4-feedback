# Architecture Spine Rubric Review

**Document reviewed:** `ARCHITECTURE-SPINE.md`  
**Reviewer role:** Independent architecture-spine rubric walker  
**Review date:** 2026-07-20  
**Altitude:** feature  
**Scope:** Plataforma de Feedbacks FIAP (Tech Challenge Fase 4)

---

## Verdict

**PASS WITH FIXES**

The spine is substantively strong: paradigm, layer model, async boundaries, event contracts, and FR mapping are well articulated. Fourteen ADs cover most high-risk divergence axes. However, the operational envelope (networking, ingress, Lambda–RDS connectivity, cron pinning) is under-specified for a feature-altitude spine that owns deploy/infra decisions, and two authorization/observability rules are soft enough that downstream epics could implement incompatible behavior without violating any AD.

---

## Rubric Checklist

### 1. Fixes real divergence points for epics/stories; misses none that matter

**Score: Partial pass**

**What is well covered**

| Divergence axis | Governed by | Assessment |
| --- | --- | --- |
| Monolith vs microservices vs API Gateway | AD-1 | Strong — single ECS deployable + SRP Lambdas |
| Layer dependency / hexagonal ports | AD-2 | Strong rule text; diagram contradiction (see §2) |
| Domain write ownership | AD-3 | Strong — API sole writer; Lambdas read-only/stateless |
| Avaliação transaction + async publish | AD-4 | Strong — after-commit, no rollback on SES/SQS failure |
| Alert payload contract | AD-5 | Strong — self-contained SQS, no DB in notification Lambda |
| Report Lambda topology | AD-6 | Strong — one Lambda, two crons, S3 key convention |
| Local Kafka vs prod SQS | AD-7 | Strong — port/adapter by profile |
| JWT auth model | AD-8 | Adequate for login; incomplete for route-level matrix |
| HTTP surface / versioning | AD-9 | Strong — `/api/v1/`, ASCII resources |
| Demo report trigger | AD-10 | Strong — manual invoke, no admin route |
| Secrets / IaC / teardown | AD-12 | Strong — Secrets Manager, CDK-only, teardown checklist |
| Lambda runtime stack | AD-13 | Strong — Spring Boot + SCF, no Web/Tomcat |
| Unit coverage gate | AD-14 | Strong — JaCoCo ≥ 90%, CI fail |

**Material gaps (would cause epic/story divergence)**

| Gap | Why it matters | Severity |
| --- | --- | --- |
| **VPC / Lambda–RDS networking** | `lambda-report` reads RDS; ECS writes RDS. Without a decided VPC layout (subnets, security groups, Lambda in VPC vs RDS public), infra and application stories will diverge on connectivity, cold-start trade-offs, and IAM. | **High** |
| **Ingress / TLS / public API URL** | AD-1 pins ECS Fargate but not ALB, ACM certificate, or HTTPS termination. API stories cannot assume a stable public base URL or health-check path placement. | **High** |
| **FR-8 listagem authorization matrix** | Capability map references "escopo Admin vs próprias" under AD-8, but AD-8 only defines roles, not which endpoints each role may call or whether ESTUDANTE sees only own avaliações. Two story teams could implement different filters. | **Medium** |
| **FR-3/FR-4 catalog mutation authorization** | No AD states ADMIN-only for Curso/Aula CRUD vs read access for ESTUDANTE. Catalog epics may diverge on 403 behavior. | **Medium** |
| **EventBridge cron expressions** | AD-6 mandates two rules in `America/Sao_Paulo` but not schedule strings (e.g., `cron(0 6 * * ? *)`). Daily vs weekly window boundaries could diverge from PRD intent. | **Medium** |
| **SQS DLQ / retry policy** | AD-4 mentions "retry/DLQ na borda" without naming maxReceiveCount, DLQ queue, or alarm. Notification reliability stories lack a shared contract. | **Medium** |
| **Bootstrap admin user / seed data** | FR-1 login requires at least one ADMINISTRADOR. No AD, convention, or deferred item covers Flyway seed vs manual Secrets Manager setup. Onboarding stories diverge. | **Medium** |
| **JWT TTL / secret rotation cadence** | AD-8 defines roles; no TTL or claim set beyond `role`. Minor but affects Postman collection and demo scripts. | **Low** |

**Conclusion:** Core domain and async boundaries are well pinned. Operational and authorization sub-matrices are the main missing divergence fixes.

---

### 2. Every AD Rule is enforceable and prevents stated divergence

**Score: Partial pass**

| AD | Enforceable? | Prevents stated divergence? | Notes |
| --- | --- | --- | --- |
| AD-1 | Yes — CDK constructs, no API Gateway REST for business API | Yes | Region and service choices verifiable in IaC |
| AD-2 | Yes — ArchUnit / package rules | **Weakened** | Mermaid shows `web -.-> infra` (dotted edge), contradicting "dependências só api → application → domain ← infrastructure". Reviewers cannot tell if `api` may wire Security adapters directly |
| AD-3 | Yes — IAM (report Lambda SELECT-only), code review | Yes | |
| AD-4 | Yes — `@Transactional` + `@TransactionalEventListener(phase=AFTER_COMMIT)` pattern | Yes | DLQ details too vague to enforce uniformly |
| AD-5 | Yes — JSON schema test / contract test on SQS payload | Yes | |
| AD-6 | Yes — single Lambda ARN in CDK, two EventBridge rules | Mostly | Cron strings not enforceable from current rule text |
| AD-7 | Yes — Maven profiles, conditional beans | Yes | |
| AD-8 | Yes — Spring Security `@PreAuthorize` / request matchers | **Partial** | Prevents "auth only in Postman" but not incomplete role matrices (FR-8, catalog) |
| AD-9 | Yes — controller path audit | Yes | |
| AD-10 | Yes — absence of admin report route | Yes | |
| AD-11 | **Weak** | **Partial** | "métricas/alarmes CloudWatch (request count, erros, latência)" lists categories but no metric names, namespaces, or alarm thresholds. Two teams could both comply yet produce incomparable observability |
| AD-12 | Yes — secret scanning, single IaC folder, teardown script | Yes | |
| AD-13 | Yes — dependency analysis excluding `spring-boot-starter-web` | Yes | |
| AD-14 | Yes — JaCoCo Maven/Gradle plugin + CI gate | Yes | |

**AD-2 diagram vs rule:** The dotted `web -.-> infra` edge is the only internal inconsistency. Either remove the edge or add an explicit exception (e.g., `@Configuration` import in `api` module only).

**AD-11:** Rule prevents "health always 200" and "métricas inventadas" only at a rhetorical level. Without named metrics (e.g., `AWS/ApplicationELB`, custom `FeedbackApi/Requests`, error rate alarm > N%), enforcement is subjective.

---

### 3. Nothing under Deferred could let two units diverge incompatibly

**Score: Pass with one watch item**

| Deferred item | Divergence risk | Assessment |
| --- | --- | --- |
| Gráficos no PDF | None | Cosmetic; tabular PDF remains compatible |
| Outbox transacional completo | Low | AD-4 already mandates publish-after-commit; deferred outbox is a future upgrade path, not a parallel MVP option. **Safe** if stories cite AD-4 |
| Refresh token / rotação JWT | None | Explicitly out of MVP |
| Multi-AZ / HA RDS | None | Non-goal; single-AZ is implicit default |
| SnapStart / Graal | None | Performance optimization only |
| CDK Java vs TypeScript | Low | AD-12 locks CDK as tool; language swap produces equivalent AWS resources. Documented assumption allows Java CDK — teams should pick one in `infra/` README, not both |
| Endpoint admin de relatório | None | Rejected by AD-10 |
| Cobertura integração/E2E | **Watch** | "Fica a critério do implementador" does not cause *architectural* incompatibility, but could let modules ship with conflicting assumptions about broker/database in tests. Acceptable at this altitude if treated as quality, not structure |

**No deferred item authorizes two incompatible runtime architectures.** The outbox entry is the closest call but is properly subordinate to AD-4.

---

### 4. Named tech is pinned / verified-looking

**Score: Partial pass**

**Well pinned**

| Technology | Pin | Looks verified |
| --- | --- | --- |
| Java | 17 | Yes |
| Spring Boot | 4.0.7 | Specific (2026-plausible) |
| Spring Cloud Function adapter | 5.0.3 | Yes, with BOM `2025.1.2` |
| PostgreSQL | 16.x (RDS) | Yes |
| JaCoCo | 0.8.15 | Yes |
| aws-cdk-lib | 2.261.0 | Yes |
| AWS region | sa-east-1 | Yes |
| Lambda runtime | java17 | Yes |

**Soft or unpinned**

| Technology | Issue |
| --- | --- |
| Resilience4j | "alinhado ao Boot / starter" — no version |
| AWS SDK for Java | "2.x" — no minor/patch |
| Apache Kafka | "3.x" — local only; minor unpinned |
| Spring Security + JWT | "alinhado ao Boot 4.0.7" — no library (e.g., jjwt vs nimbus) |
| GitHub Actions | Named but no runner/OS pin |
| ECS Fargate | No CPU/memory/task definition baseline |

For a feature spine, core framework pins are good. Operational sizing and secondary library pins are loose but not blocking unless stories need them now.

---

### 5. Covers PRD capabilities FR-1..17

**Score: Pass**

Header `binds` lists FR-1 through FR-17. Capability map accounts for each:

| FR | Covered in map | Governed |
| --- | --- | --- |
| FR-1 Login JWT | Yes | AD-8, AD-9 |
| FR-2 Autorização papéis | Yes | AD-8 |
| FR-3–FR-4 Catálogo | Yes | AD-2, AD-3 |
| FR-5–FR-6 Inscrição | Yes | AD-2, AD-3 |
| FR-7–FR-9 Avaliação + Urgência | Yes | AD-3, AD-4, AD-5 |
| FR-8 Listagem | Yes | AD-8 (incomplete rule) |
| FR-10 Alerta ALTA | Yes | AD-1, AD-5, AD-7, AD-13 |
| FR-11 Relatório diário | Yes | AD-6, AD-10, AD-13 |
| FR-12 Relatório semanal | Yes | AD-6 |
| FR-13 Health | Yes | AD-11 |
| FR-14 Métricas/alarmes | Yes | AD-11 |
| FR-15 Deploy | Yes | AD-1, AD-12 |
| FR-16 Roteiro | Yes | AD-10 |
| FR-17 (weekly report variant) | Yes | Grouped with FR-11/FR-12 |

FR-17 is bound in AD-6 and the map. No FR is orphaned.

---

### 6. Every owned dimension decided, deferred, or open question — operational envelope

**Score: Fail on operational envelope (overall partial pass)**

**Decided**

- Paradigm, module layout, async boundaries
- Local vs AWS broker (AD-7)
- IaC tool (CDK), secrets store, teardown intent (AD-12)
- CI/CD path (GHA → ECR → cdk deploy)
- Profiles `local` / `aws`
- Observability minimum (AD-11, albeit vague)
- Report manual invoke for demo (AD-10)

**Explicitly deferred (appropriate)**

- HA / Multi-AZ
- SnapStart / Graal
- Graphs in PDF (OQ-5)
- Refresh token
- Outbox pattern upgrade

**Open questions referenced**

- OQ-3 (demo invoke) → resolved by AD-10 `[ADOPTED]`
- OQ-4 (HTTP surface) → resolved by AD-9 `[ADOPTED]`
- OQ-5 (PDF graphs) → resolved in conventions `[ADOPTED OQ-5]`

**Missing decisions (should be AD, convention row, deferred, or OQ)**

| Dimension | Status | Recommendation |
| --- | --- | --- |
| VPC topology (ECS, RDS, Lambda-report) | **Undecided** | Add AD or infra convention: e.g., single VPC, private subnets for RDS, Lambda in VPC for report, ECS tasks with SG access to RDS |
| Public ingress (ALB + ACM + HTTPS) | **Undecided** | Add to AD-1 or AD-12: ALB terminates TLS; ECS service behind target group |
| EventBridge cron schedules | **Undecided** | Pin in AD-6 or conventions table |
| SQS main + DLQ naming and redrive | **Partial** (mentioned, not specified) | Extend AD-4 or AD-5 |
| SES identity (domain/email verification) | **Undecided** | Convention or AD-12 bullet |
| ECS desired count / min tasks for demo | **Undecided** | Convention: `desiredCount=1` for MVP |
| RDS instance class / storage | **Undecided** | Defer with cost note or pin `db.t4g.micro` |
| Bootstrap ADMIN user | **Undecided** | Flyway seed AD or convention |
| Authorization matrix per FR | **Partial** | Extend AD-8 with route table |
| Environment count (local + aws only?) | **Implicit** | State explicitly: no staging |

The spine owns FR-15 (deploy) and binds NFR security/ops via AD-11/AD-12 but leaves the highest-risk infra wiring implicit. That is the primary altitude gap.

---

### 7. No template comments; diagrams valid mermaid

**Score: Pass**

- No `TODO`, `TBD`, `{{...}}`, or placeholder template comments found.
- Four mermaid blocks present; all use valid syntax (`flowchart`, `erDiagram`).
- Diagram content is coherent with prose (minor AD-2 edge issue is semantic, not syntactic).

---

## Summary of Findings by Severity

| # | Severity | Finding |
| --- | --- | --- |
| F1 | **High** | Operational envelope incomplete: VPC, security groups, Lambda–RDS path, ALB/HTTPS not decided — epics for `infra/`, `lambda-report`, and `api` can diverge incompatibly. |
| F2 | **High** | FR-8 and catalog (FR-3/FR-4) authorization matrix not ruled in AD-8; only roles defined. |
| F3 | **Medium** | AD-11 observability rule too vague to enforce comparable metrics/alarms across modules. |
| F4 | **Medium** | AD-6 EventBridge schedules and AD-4 SQS DLQ policy unpinned. |
| F5 | **Medium** | AD-2 mermaid contradicts dependency rule (`web -.-> infra`). |
| F6 | **Medium** | Bootstrap ADMIN user / seed strategy absent for FR-1. |
| F7 | **Low** | Secondary stack versions soft-pinned (Resilience4j, AWS SDK 2.x, JWT library). |

---

## Recommended Fixes (minimal to reach full pass)

1. **Add AD-15 (Network & ingress)** or extend AD-1/AD-12: single VPC, RDS private, ECS + Lambda-report SG rules, ALB + ACM HTTPS, public API only via ALB.
2. **Extend AD-8** with a route authorization table: e.g., catalog write = ADMINISTRADOR; inscrição + avaliação create = ESTUDANTE; listagem = ADMIN all / ESTUDANTE own rows only.
3. **Pin AD-6 schedules** in conventions: daily cron time and weekly day/time in `America/Sao_Paulo`.
4. **Harden AD-11** with metric namespace names and at least one error-rate or 5xx alarm threshold.
5. **Fix AD-2 diagram** — remove `web -.-> infra` or document the allowed exception.
6. **Add bootstrap convention**: Flyway seed for initial ADMIN (password from Secrets Manager) or document one-time CLI step in `docs/`.
7. **Optional:** Pin `db.t4g.micro`, `desiredCount=1`, SQS DLQ `maxReceiveCount=3`.

---

## Checklist Scorecard

| Criterion | Result |
| --- | --- |
| Fixes real divergence points | Partial — domain/async strong; ops/auth gaps |
| AD rules enforceable | Partial — AD-2 diagram, AD-11 soft |
| Deferred items safe | Pass |
| Tech pinned | Partial — core yes, secondary loose |
| FR-1..17 covered | Pass |
| All dimensions decided/deferred/OQ | Partial — ops envelope gap |
| No templates; valid mermaid | Pass |

---

*End of rubric review.*
