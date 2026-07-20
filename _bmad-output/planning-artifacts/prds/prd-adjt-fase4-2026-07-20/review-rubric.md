# PRD Quality Review — Plataforma de Feedbacks FIAP

## Overall verdict
This is a strong Fast-path academic PRD: the thesis (feedback that *acts* via urgência + relatório, delivered as a filmable AWS demo by 28/07/2026) holds, Non-Goals and Constraints are honest, and FRs carry testable consequences an engineer can implement against. The main usefulness risk is incomplete done-ness on catálogo fields and re-avaliação (Open Questions 1–2), plus a few UJ↔FR cross-reference slips that would confuse story extraction if left unfixed — not green-light blockers at hobby/academic stakes.

## Decision-readiness — strong
A solo decision-maker can act. Form-factor, deadline, cost, and what is deliberately not built are stated as decisions, not soft considerations: “sem portal web no MVP: tudo via API REST (Postman) e e-mail” (§1); Non-Goals name UI, multi-tenant/SSO/LMS, MSK, HA beyond demo, and 24/7 ops (§5); Constraints lock prazo **28/07/2026**, ~USD 20–30/mês, SES sandbox, `sa-east-1`, teardown (§10). The addendum’s “Mudança vs Product Brief” table records Brief→PRD choices as confirmed with Thiago (Curso/Aula + inscrição IN; health/métricas/PDF/roteiro IN) rather than burying them. Open Questions (§12) are genuinely open (campos de Curso/Aula, re-avaliação, trigger manual do relatório, path versionado, conteúdo do PDF) — none are rhetorical with the answer in the next sentence. Counter-metrics SM-C1/SM-C2 (§7) push back on enterprise overbuild. No `[NOTE FOR PM]` callouts appear; at these stakes Open Questions + Assumptions Index absorb that role adequately.

### Findings
- **low** Unused `[NOTE FOR PM]` channel (§12 / throughout) — Real tensions (ex.: OQ-3 disparo manual do relatório para o vídeo) live only as Open Questions, not as PM escalation callouts. Fine for solo academic; weaker if this PRD later feeds a multi-person handoff. *Fix:* Optional — add one `[NOTE FOR PM]` on OQ-3 linking demo risk to FR-11/FR-12, or leave as-is for Fast path.

## Substance over theater — strong
Content is earned for the product shape. Vision is swap-test resistant: “lacuna entre ‘guardar comentários’ e **agir**” plus API-only MVP and academic AWS learning by a fixed date (§1) would not drop unchanged into a generic edtech PRD. Three UJ protagonists (Ana, Bruno, Thiago) each drive FR sets and the YouTube roteiro (§2.3, §11) — not persona furniture. JTBD are terse but decision-linked (matricular + avaliar; catálogo + alertas; provar cloud no vídeo) (§2.1). NFRs avoid empty scalability/security boilerplate: JWT + secrets out of code, Avaliação persistence survives transient e-mail/queue failure, teardown/operability, and an explicit demo latency assumption (§8). Success Metrics measure enunciado coverage, vídeo, repo docs, and cost (§7) — matching the academic thesis rather than vanity DAU. No innovation-theater differentiation section; novelty is not claimed.

### Findings
_(none — dimension is strong)_

## Strategic coherence — strong
Thesis is explicit and features follow it: catálogo → inscrição → Avaliação → Urgência → Alerta ALTA → Relatório semanal → observabilidade → deploy/roteiro (§4.1–4.8). MVP scope kind is coherent problem-solving + academic-delivery hybrid: In Scope mirrors the enunciado and demo obligations (§6.1); Out of Scope gives reasons (prazo, custo, “enunciado aceita demo API”) (§6.2). Primary SMs validate the thesis (SM-1 enunciado; SM-2 vídeo até a data; SM-3 repo) rather than activity proxies; secondary SMs (custo, setup Postman &lt; 10 min) and counter-metrics against QPS/HA and Lambda sprawl keep the arc honest (§7). Addendum reinforces the same bet (SQS prod vs Kafka local for cost/aprendizado).

### Findings
_(none — dimension is strong)_

## Done-ness clarity — adequate
Most FRs meet the testable-consequence bar: login reject paths (FR-1), role denies (FR-2), missing Curso on Aula create (FR-3), inscrição duplicate/conflict and Avaliação gate (FR-5–FR-6), nota bounds and Urgência boundaries 4/5/7/8 (FR-7, FR-9), ALTA-only alert and SRP Lambda (FR-10), relatório aggregates + HTML/PDF (FR-11–FR-12), pipeline evidence (FR-15), roteiro scene list (FR-16). That pattern will carry story creation for the core flow. Gaps that leave “done” under-specified: Curso/Aula payload beyond id/nome is only an Open Question (§12.1) while FR-3 consequences only require “listável/consultável” and Curso existente (§4.2); whether the same Estudante may re-avaliar a Aula is Open Question 2 with no FR-7 consequence (§12.2, §4.4). NFR Performance leads with the adjective “latência ‘aceitável para Postman’” before the ASSUMPTION bound `p95 API < 2s` (§8) — workable for demo stakes but soft if someone treats the adjective as the requirement. FR-14’s “suficientes para a gravação” / “ao menos um painel” is appropriately demo-scoped, not theater.

### Findings
- **medium** Curso/Aula field contract unspecified (§4.2 FR-3; §12 OQ-1) — Engineer cannot know minimum create/list payload; only “id, dados essenciais” on FR-4. Blocks clean OpenAPI/stories for catálogo. *Fix:* Resolve OQ-1 (e.g. nome obrigatório; descrição opcional) and add one consequence under FR-3/FR-4.
- **medium** Re-avaliação da mesma Aula undecided (§4.4 FR-7; §12 OQ-2) — No reject/idempotent/update rule; duplicate Avaliações would silently affect média/qty no Relatório (FR-11). *Fix:* Pick one rule (reject | allow N | upsert) as `[ASSUMPTION]` or FR-7 consequence; index it.
- **low** Performance NFR adjective-first (§8) — “aceitável para Postman” is untestable without the ASSUMPTION; a skimming reader may miss the p95 bound. *Fix:* Lead with the numeric ASSUMPTION (or drop the adjective).

## Scope honesty — strong
Omissions do real work. Non-Goals (§5) and Out of Scope for MVP (§6.2) are explicit, with rationale (UI, MSK/HA, multi-admin inboxes, refresh-token/federação). Demo-only manual relatório trigger is called out as ASSUMPTION rather than smuggled in (§6.2). Inline `[ASSUMPTION: …]` tags appear on Admin único/SES, inscrição Curso+Aula, duplicata, `aulaId`, listagem do Estudante, janela/cron do relatório, métricas mínimas, health+DB, duração do vídeo, p95 (§3, §4, §8) and round-trip in Assumptions Index (§13). Open-items density (5 OQs + ~11 assumptions) fits low–medium academic stakes: high enough to stay honest, not a green-light blocker. De-scoping vs Brief is documented in the addendum table, not silent.

### Findings
_(none material — dimension is strong)_

## Downstream usability — strong
For a chain-top Fast PRD (architecture, epics/stories, vídeo), sections extract cleanly. Glossary anchors domain nouns used consistently across UJs/FRs (§3). FR-1…FR-16, UJ-1…UJ-5, SM-1…SM-5 + SM-C1/C2 are contiguous and unique. UJs name protagonists inline (Ana, Bruno, Thiago) with context (§2.3). API Surface maps capacidades → FRs (§9); roteiro maps cenas → FRs/SMs (§11). Mechanism (SQS, Lambda, EventBridge, ECS, stack) correctly lives in `addendum.md`, keeping the PRD a capability contract (§0). Minor cross-ref slips (below) are fixable and do not erase extractability; calibrated as strong with findings, not thin.

### Findings
- **medium** UJ↔FR cross-references misaligned (§2.3) — UJ-1 (Ana) says “Realiza FR-3–FR-7” but FR-3 is Admin cria Curso/Aula; Ana’s path is FR-4–FR-7 (listar, inscrever, avaliar). UJ-2 (Bruno catálogo) says “FR-1–FR-2, FR-8” and omits FR-3/FR-4 that the journey text describes. Story slicing from UJs alone would mis-attribute work. *Fix:* UJ-1 → FR-4–FR-7 (and FR-1 for login); UJ-2 → FR-1–FR-4, FR-8.
- **low** SM-1 validation list skips FR-9 (§7) — “Valida FR-7, FR-10–FR-15” omits FR-9 (classificação de Urgência) though SM-1 text requires alerta ALTA / urgência. *Fix:* Include FR-9 (and keep FR-16 under SM-2 as today).

## Shape fit — strong
Shape matches a solo Tech Challenge: capability API spec + academic delivery (deploy, monitoramento, roteiro YouTube), not a consumer-growth PRD. Rubric guidance for hobby/solo (“rigor light, substance bar still applies”) is met — substance is present; ceremony is not inflated. UJs are load-bearing for the demo narrative (Ana/Bruno/Thiago), not over-formalized for a single-operator tool. SMs are operational/academic (enunciado, vídeo, custo, teardown) rather than forced user-facing engagement metrics. Brownfield accuracy N/A (greenfield). Stack/infra parked in addendum avoids forcing a mechanism-shaped PRD. Non-Users (§2.2) correctly exclude browser end-users and real multi-tenant institutions.

### Findings
_(none — dimension is strong)_

## Mechanical notes
- **Glossary:** Nouns (Estudante, Administrador, Curso, Aula, Inscrição, Avaliação, Urgência, Alerta de urgência, Relatório semanal, API, Health check, Métricas de mercado) used consistently; “Métricas de mercado” is slightly awkward naming but stable.
- **ID continuity:** FR-1–FR-16, UJ-1–UJ-5, SM-1–SM-5, SM-C1–SM-C2 — no gaps/duplicates. Unresolved cross-refs: UJ-1/UJ-2 FR ranges (see Downstream findings); UJ-5 cites “FR-13–FR-15” while roteiro/FR-16 is the artifact itself (acceptable).
- **Assumptions Index roundtrip:** Inline tags in §3, FR-5, FR-7, FR-8, FR-11, FR-13, FR-16, §6.2, §8 appear in §13; index entries resolve to those locations. No orphans spotted.
- **UJ protagonists:** All five UJs name Ana / Bruno / Thiago with inline context — pass.
- **Required sections (academic / low–medium stakes):** Vision, users/UJs, Glossary, FRs, Non-Goals, MVP, SMs (+ counters), NFRs, Constraints, Open Questions, Assumptions Index — present; addendum covers stack. HTML entities (`&lt;`) in §7 SM-5 and §8 are markdown escaping artifacts, not content issues.
- **Addendum:** Brief deltas, stack table, async flow, budget/teardown, enunciado PDF reference — coherent companion; no conflict with PRD capabilities beyond intentional mechanism detail.
