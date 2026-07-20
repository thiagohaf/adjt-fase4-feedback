# Reconciliation Review — Architecture Spine vs PRD + Addendum

**Date:** 2026-07-20  
**Spine:** `ARCHITECTURE-SPINE.md` (draft)  
**Sources:** `prd.md`, `addendum.md` (prd-adjt-fase4-2026-07-20)  
**Method:** Line-by-line trace of FRs, NFRs, constraints, assumptions, success metrics, user journeys, and addendum mechanisms against spine ADs, conventions, stack, capability map, and Deferred table.

---

## Verdict

**Mostly aligned — fit for build-substrate with targeted gaps.**

The spine correctly binds all seventeen functional requirements (FR-1–FR-17), resolves open questions OQ-3/OQ-4/OQ-5, and encodes the addendum’s cloud model (ECS + two SRP Lambdas, SQS prod, Kafka local, SES/S3, CDK, teardown). Structural AD choices (modular monolith, publish-after-commit, self-contained alert payload, single parametrized report Lambda) are sound and traceable.

What did **not** land falls into three buckets:

1. **Intentional deferrals / supersessions** — explicitly recorded in spine `Deferred` or user decisions in `.memlog.md`; not defects.
2. **Quiet requirements the AD structure dropped** — delivery tone, verification metrics, operational guardrails, and domain-level validation that PRD states but no AD/convention carries.
3. **Mechanism gaps** — addendum/PRD specifics (cron times, aggregate schemas, sizing, Resilience4j usage) absent from spine despite being testable consequences.

**Recommendation:** Proceed to implementation; patch spine (or first epic stories) for **report cron/aggregates**, **infra sizing/cost**, and **Resilience4j/NFR performance** before stories split report and observability work.

---

## Coverage Summary

| Area | PRD / Addendum | Spine | Status |
|------|----------------|-------|--------|
| FR-1–FR-17 capability binding | All listed | Frontmatter `binds` + Capability Map | ✅ Complete |
| OQ-3 manual report trigger | Console/CLI invoke | AD-10 `[ADOPTED]` | ✅ Resolved |
| OQ-4 API paths | Versioned ASCII | AD-9 `[ADOPTED]` `/api/v1/avaliacoes` | ✅ Resolved |
| OQ-5 PDF content | Tables, no chart | Conventions + Deferred | ✅ Resolved |
| Cloud model (container + serverless) | Addendum + SM-2 | AD-1, diagram, paradigm | ✅ Complete |
| Kafka local / SQS prod | Addendum | AD-7 | ✅ Complete |
| JWT roles ESTUDANTE \| ADMINISTRADOR | PRD + addendum | AD-8, conventions | ✅ Complete |
| Single writer domain (API) | Implicit in flows | AD-3 | ✅ Complete |
| Alert async + no rollback | FR-9, FR-10, NFR | AD-4, AD-5 | ✅ Complete |
| Teardown post-delivery | Constraints §10, addendum | AD-12 | ✅ Partial (no cost) |
| IaC CDK only | User decision (memlog) | AD-12 | ✅ Supersedes addendum ambiguity |
| Spring Boot version | Addendum 3.4.5 | Stack 4.0.7 | ⚠️ Intentional supersession |
| JaCoCo ≥ 90% | Not in PRD | AD-14 | ➕ Architecture addition |

---

## Intentional Deferrals and Supersessions

These are **not gaps**. They are either explicit in spine `Deferred`, adopted AD prevents, or recorded user decisions.

| Item | Source | Spine handling |
|------|--------|----------------|
| Gráficos no PDF | OQ-5, PRD §12 | `Deferred`; convention “sem gráfico no MVP” |
| Outbox transacional completo | Architecture judgment | `Deferred`; AD-4 publish-after-commit + DLQ |
| Refresh token / JWT rotation | PRD §6.2 out of scope | `Deferred` |
| Multi-AZ / HA RDS | PRD §5 non-goals | `Deferred`; AD-1 prevents MSK/HA inflation |
| SnapStart / Graal on Lambdas | NFR cold-start tolerance | `Deferred` |
| CDK TypeScript vs Java | Memlog assumption | `Deferred`; AD-12 binds CDK not language |
| Admin HTTP endpoint for report | OQ-3 | `Deferred`; AD-10 rejects |
| Integration / E2E test gate | Architecture judgment | `Deferred`; AD-14 unit-only |
| Spring Boot 3.4.5 → 4.0.7 | Addendum vs memlog user pin | Stack 4.0.7; addendum line superseded |
| Terraform vs CDK | Memlog user pin | AD-12 CDK only |
| Plain SDK Lambda handlers | Memlog user pin | AD-13 Spring Boot + SCF |
| Third Lambda 1:1 per report job | Addendum alternative | AD-6 single parametrized Lambda |
| MSK in production | PRD §5, addendum | AD-1, AD-7 prevents |
| Portal web / UI | PRD non-goals | Out of spine scope (correct) |
| Multi-tenant / SSO / LMS | PRD non-goals | Out of spine scope (correct) |

---

## Gaps — Requirements Not Landed in Spine

### G1. Report schedule mechanics (FR-11, FR-17) — **High**

**PRD states (testable):**

- Relatório **diário:** disparo **08:00 `America/Sao_Paulo`**, janela = **dia civil anterior**.
- Relatório **semanal:** disparo **segunda 08:00 `America/Sao_Paulo`**, janela = **7 dias civis anteriores**.

**Spine:** AD-6 says “duas regras EventBridge (`America/Sao_Paulo`)” and `periodo=diario|semanal` but **no clock time, no weekday, no window definition**.

**Risk:** Implementer picks wrong cron; weekly report window misaligned with FR-11 acceptance tests and video demo.

---

### G2. Report aggregate schema — daily vs weekly (FR-11, FR-17, FR-12) — **High**

**PRD states (testable):**

| Report | Required aggregates |
|--------|---------------------|
| Diário | Média de notas do dia; **quantidade total** do dia; quantidade **por nível de Urgência** |
| Semanal | Média de notas; **quantidade de Avaliações por dia**; quantidade **por nível de Urgência** |

**FR-12 also requires:** e-mail HTML legível com agregados + **identificação do tipo (diário vs semanal) e do período**; PDF distinguível por nome/caminho.

**Spine:** Convention “PDF: tabelas com agregados obrigatórios” and S3 key `relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf` — **does not enumerate different aggregate shapes per period**, nor HTML identification rules.

**Risk:** Weekly report shipped without per-day breakdown; email body missing period label; demo fails SM-1 / enunciado.

---

### G3. Infra sizing and cost guardrails (addendum, Constraints §10, SM-4) — **Medium**

**PRD / addendum:**

- ECS Fargate **0.25 vCPU**
- RDS **db.t3.micro** (addendum: “privilegiar free tier”)
- Custo alvo **USD 20–30/mês**
- Teardown checklist **and** script/documentation

**Spine:** AD-12 covers secrets, CDK, teardown checklist/script at rule level. Stack lists ECS/RDS generically. **No vCPU, instance class, or cost ceiling.**

**Risk:** Oversized Fargate/RDS blows SM-4; cost narrative in video (roteiro cena 13) lacks architectural anchor.

---

### G4. Resilience4j operational rules (NFR §8 Resiliência, addendum stack) — **Medium**

**PRD:** “falhas transitórias em integrações externas (e-mail/fila) não corrompem a Avaliação já persistida; **retry/circuit conforme addendum**.”

**Spine:** Resilience4j in Stack; AD-4 covers non-rollback and DLQ at publish border. **No AD for retry/circuit breaker on SQS publish, SES, or Lambda-side SES failures.**

**Risk:** Inconsistent retry behavior; circuit breaker omitted entirely despite PRD NFR wording.

---

### G5. Performance NFR — p95 under demo load (PRD §8) — **Low**

**PRD:** `[ASSUMPTION: p95 da API < 2s sob carga de demo; sem SLO de produção.]`

**Spine:** Not referenced. AD-11 metrics include latency but no target.

**Risk:** Low for academic MVP; matters if video shows latency alarm or SM-C1 is ignored by over-provisioning.

---

### G6. Domain validation and entity field rules (FR-3, FR-7) — **Medium**

**PRD testable consequences not in conventions/ADs:**

- Curso/Aula: **nome** obrigatório; **descrição** opcional (FR-3)
- Avaliação: **nota** 0–10 validation rejection (FR-7)
- Urgency boundary values 4, 5, 7, 8 (FR-9) — partially covered by urgency enum convention

**Spine:** ER diagram and urgency enum only. No validation AD or DTO constraint convention.

**Risk:** Stories implement inconsistent validation; OpenAPI/Postman drift.

---

### G7. Authorization nuance — FR-8 student scope (FR-2, FR-8) — **Medium**

**PRD:**

- Estudante **lista apenas as próprias** Avaliações `[ASSUMPTION]`
- Administrador **não precisa se inscrever** para consultar catálogo/Avaliações (FR-2)

**Spine:** Capability map notes “escopo Admin vs próprias” under FR-8; AD-8 covers roles generically. **No explicit rule preventing cross-student data leak on list endpoints.**

**Risk:** Implementer exposes global list to Estudante; fails FR-8 acceptance.

---

### G8. SES sandbox and single-admin recipient (PRD §3, §10, assumptions) — **Low**

**PRD:** One demo Administrador; e-mail = **endereço verificado no SES**; sandbox OK; no unverified recipients (§5 non-goal).

**Spine:** AD-12 “e-mail admin” in Secrets Manager. **SES verification/sandbox constraint not stated.**

**Risk:** Demo email failures during recording; not an architecture blocker if documented in `docs/`.

---

### G9. Observability specificity (FR-13, FR-14, Glossary) — **Low**

**PRD testable / glossary minimum:**

- Health: **“up”** when healthy; DB connectivity check
- Metrics: request count, **error rate / 4xx–5xx**, **latency p95 (or CloudWatch equivalent)**
- **Alarm or monitoring configuration documented** (enunciado)
- Video must show panel/alarm with demo traffic

**Spine:** AD-11 — health with DB, CloudWatch metrics (request count, erros, latência), request-id logging. **Missing:** p95 explicit, health response shape, **documentation deliverable** for alarm config.

---

### G10. Delivery and verification tone dropped by AD structure — **Medium (process, not runtime)**

The spine optimizes for **build substrate** (ADs, stack, structure). The PRD’s **success metrics, roteiro, and counter-metrics** are largely absent — acceptable for a spine if carried by stories/docs, but currently **unbound**:

| ID | Requirement | In spine? |
|----|-------------|-----------|
| SM-1 | Enunciado coverage checklist | Implicit via FR map only |
| SM-2 | Video shows **justificativa do modelo cloud** | Not bound (addendum + roteiro cena 2) |
| SM-3 | Repo documents architecture, deploy, monitoring | `docs/` in seed; no AD |
| SM-4 | Cost ≤ USD 20–30 + teardown | Teardown only (G3) |
| SM-5 | Postman UJ-1/UJ-2 reproducible **< 10 min** (seed + tokens) | Not bound |
| SM-C1 | Do not optimize QPS/HA | Not in prevents (partially via Deferred) |
| SM-C2 | Do not inflate Lambda count beyond SRP | AD-6 aligns; not explicit |
| FR-16 / §11 | Roteiro: **13 scenes**, **8–15 min**, pré-gravação checklist | AD-10 covers one scene (manual invoke) |
| §9 | Breaking changes → update Postman + roteiro same PR | Not bound |
| §10 | Prazo **28/07/2026**, **solo** team | Not in spine |
| UJ-1–UJ-5 | User journeys | Not mapped (traceability gap) |

**Tone lost:** Academic delivery contract (filmable, cost-conscious, solo-scoped) lives in PRD §1, §7, §10–11 but does not constrain architecture decisions beyond teardown.

---

### G11. Postman as official client — **Low**

**PRD:** Cliente oficial do MVP é Postman; diagram shows Postman.

**Spine:** Diagram only. No convention for collection location, seed users, or auth flow documentation — couples to SM-5 gap.

---

### G12. Enunciado PDF reference (addendum) — **Low**

**Addendum:** `docs/ADJT - Fase 4 - Tech Challenge_rev.pdf` as source for `POST /avaliação` and minimum email/report content.

**Spine:** Structural seed `docs/` generic. **No trace link to enunciado** for implementers validating minimum content against official brief.

---

### G13. Artifact versioning (FR-15) — **Low**

**PRD:** Pipeline documented and executable; **artefatos versionados** (imagem/pacote) published in deploy flow.

**Spine:** GHA → ECR / Lambda → `cdk deploy`. **No tagging/versioning convention.**

---

## Quiet Requirements — Tone and Constraints the AD Layer Dropped

These are not always testable FRs but shape **correct delivery** and **what not to build**:

1. **“Agir, não só guardar”** (PRD §1 vision) — urgency + alert + reports; spine captures mechanism, not product narrative for video opening (roteiro cena 1).
2. **Learning-over-enterprise** — PRD §10 “preferir escolhas AWS explicáveis no vídeo”; spine explains model in AD-1 but doesn’t bind **explainability** as deliverable (SM-2).
3. **Demo-first, not 24/7 production** — non-goals + teardown; spine AD-12 partial; **cost ceiling** missing (SM-4).
4. **Counter-metrics SM-C1/C2** — guardrails against HA inflation and Lambda sprawl; only partially reflected in `Deferred` / AD-6, not as explicit prevents.
5. **Solo developer scope calibration** — PRD Constraints §10; affects story sizing; absent from spine (expected at planning layer).
6. **No UI ever in v1** — correct omission from runtime ADs; worth noting for boundary tests (no accidental admin UI for report trigger — AD-10 handles).
7. **Breaking change discipline** — PRD §9 quiet process rule; dropped.
8. **Assumption: duplicate inscrição → conflict (409)** — spine error convention covers 409; **idempotency choice documented in PRD** but not stated as rule (minor).
9. **MÉDIA (PRD) vs `MEDIA` (spine enum)** — ASCII enum in spine is fine; **document mapping** to avoid PRD/enunciado wording mismatch in emails/PDFs.

---

## What Landed Well (No Action Needed)

- Full FR binding and async/sync split (paradigm table + capability map).
- Domain ownership and evaluation mutation transaction (AD-3, AD-4).
- Self-contained alert event contract exceeding FR-10 minimum fields (AD-5).
- Auth model and Lambda IAM boundary (AD-8).
- HTTP surface resolution OQ-4 (AD-9).
- Demo report trigger OQ-3 (AD-10).
- Secrets, CDK-only IaC, teardown intent (AD-12).
- Lambda Spring Cloud Function stack parity (AD-13).
- Layered dependency rule (AD-2).
- Error envelope and HTTP status conventions.
- Timezone for reporting windows (`America/Sao_Paulo` in conventions).
- Non-goals for MSK, multi-AZ, UI — enforced via AD prevents and Deferred.

---

## Recommended Spine Patches (Priority Order)

1. **AD-6 extension or new AD-15:** Cron expressions (`cron(0 8 * * ? *)` daily; weekly Monday 08:00 SP), window definitions (previous civil day; previous 7 civil days).
2. **Convention row — Report content:** Enumerate daily vs weekly aggregate columns; HTML subject/body must include `periodo` + date range.
3. **Stack or AD-12 footnote:** ECS 0.25 vCPU, RDS db.t3.micro, cost target USD 20–30/mês.
4. **AD-4 or new AD — Resilience:** Resilience4j retry/circuit on SQS publish (API) and SES send (Lambdas); align with NFR §8.
5. **AD-8 extension:** Estudante list Avaliações scoped to `sub`/`userId`; Admin global read without enrollment.
6. **Conventions — Validation:** Curso/Aula/Avaliação field rules from FR-3/FR-7.
7. **Docs binding (light):** `docs/` must include roteiro (§11), teardown, Postman collection, alarm config screenshot/list — supports SM-2, SM-3, FR-14, FR-16 without new runtime AD.

---

## Traceability Matrix — FR → Spine

| FR | Spine anchor | Gap? |
|----|--------------|------|
| FR-1 | AD-8, AD-9, map | — |
| FR-2 | AD-8, map | G7 partial |
| FR-3 | AD-2, AD-3, map | G6 |
| FR-4 | AD-9, map | G6 (stable representation) |
| FR-5 | AD-3, errors 409 | — |
| FR-6 | AD-4, ER diagram | — |
| FR-7 | AD-4, conventions | G6 |
| FR-8 | Map only | G7 |
| FR-9 | Conventions urgency | — |
| FR-10 | AD-4, AD-5, AD-13 | — |
| FR-11 | AD-6, map | G1, G2 |
| FR-12 | AD-6, S3 convention | G2 |
| FR-13 | AD-11 | G9 minor |
| FR-14 | AD-11 | G9 |
| FR-15 | AD-12, stack CI | G13, G10 |
| FR-16 | AD-10, docs/ | G10 |
| FR-17 | AD-6, map | G1, G2 |

---

## Conclusion

The architecture spine is a **faithful structural translation** of the PRD’s cloud/serverless model and functional scope. Open questions from the PRD are closed in AD-9/AD-10/conventions. Intentional deferrals are well labeled.

**True gaps** concentrate in **report scheduling and aggregate contracts**, **infra sizing/cost**, **Resilience4j behavior**, and **authorization/validation detail** — areas the AD-centric spine format tends to thin out. **Delivery verification** (SM metrics, roteiro, Postman reproducibility) is appropriately light in a build-substrate doc but should appear in the first planning stories or a companion `docs/` checklist so SM-2/SM-3/SM-5 are not lost during implementation.

**Verdict for Reviewer Gate:** **Approve with minor spine amendments** (G1–G3 minimum) before epics for `lambda-report`, EventBridge, and infra CDK.
