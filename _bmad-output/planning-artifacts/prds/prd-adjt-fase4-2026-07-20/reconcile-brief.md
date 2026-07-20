# Reconcile Brief → PRD

**Input:** `briefs/brief-adjt-fase4-2026-07-19/` (`brief.md`, `addendum.md`, `.memlog.md`)  
**Targets:** `prd.md`, `addendum.md` (este workspace)  
**Data:** 2026-07-20

Status: **Covered** | **Gap** | **Superseded** | **Qualitative-drop**

---

## Visão e problema

1. **Visão — feedback de aulas on-line com alerta crítico + relatório semanal**  
   Brief: *“estudantes enviam avaliações… administradores recebem alertas… e relatórios semanais”*  
   → **Covered** — `prd.md` §1 Vision: Estudantes avaliam Aulas; Administradores descobrem críticos na hora e qualidade agregada na semana.

2. **Problema — captura + triagem de urgência + visão agregada (não só armazenar)**  
   Brief: *“captura, triagem de urgência e visão agregada… não apenas armazenar comentários”*  
   → **Covered** — `prd.md` §1: *“resolver a lacuna entre ‘guardar comentários’ e agir”*.

3. **Visão de evolução (pós-MVP): dashboard, auth por perfil, analytics tempo real, LMS**  
   Brief §Vision; memlog: foco acadêmico neste ciclo.  
   → **Covered** (futuro) / parcial **Superseded** — `prd.md` §1 mantém dashboard/LMS/analytics como evolução; auth por perfil (Estudante/Admin) entrou no MVP (`prd.md` §4.1, addendum PRD “Auth na API”).

4. **Visão deste ciclo — fluxo completo código → cloud → serverless → observabilidade / portfólio**  
   Brief §Vision.  
   → **Covered** — `prd.md` §1 última frase; §11 roteiro; SM-3.

---

## Contexto acadêmico e restrições humanas

5. **Entrega Tech Challenge Fase 4; prazo 28/07/2026**  
   Brief Executive Summary + Success Criteria.  
   → **Covered** — `prd.md` §0, §1, §7 SM-2, §10 Constraints.

6. **Desenvolvedor solo (sem grupo); decisões concentradas**  
   Brief addendum *“Solo — sem grupo”*; memlog *“solo”*.  
   → **Gap** — PRD menciona autor ThiagoFerreira (§0) mas **não registra “solo / sem grupo” como restrição operacional** (impacto em escopo/prazo).

7. **Prioridade: aprender AWS + nota + custo mínimo (pago pelo próprio)**  
   Brief Executive Summary; addendum *“Prioridade: aprender AWS + nota… + custo mínimo”*.  
   → **Covered** — `prd.md` §1, §10 *“Aprendizado… escolhas AWS explicáveis”*; SM-4 custo.

8. **Demonstração via vídeo gravado (orientação do desafio)**  
   Brief; Success Criteria acadêmicos.  
   → **Covered** / expandido — `prd.md` FR-16, §11 roteiro YouTube; SM-2.

9. **Repo documentado (código, arquitetura, deploy, monitoramento, funções)**  
   Brief Success Criteria acadêmicos.  
   → **Covered** — SM-3; FR-15 consequences; §0 propósito.

10. **Infra AWS provisionada e explicada (modelo cloud escolhido)**  
    Brief Success Criteria pessoais.  
    → **Gap** — PRD exige diagrama/arquitetura no vídeo (§11 cena 2) e docs (SM-3), mas **não exige explicitamente narrar/justificar o “modelo cloud”** (ex.: FaaS vs container vs PaaS) como critério de sucesso.

---

## Personas e experiência

11. **Estudante — avaliar de forma simples (uma chamada HTTP)**  
    Brief Who This Serves.  
    → **Superseded** (expandido) — `prd.md` UJ-1 / FR-5–FR-7: login + inscrição Curso/Aula **antes** de avaliar (não é mais “uma chamada” isolada). Documentado no addendum PRD *“Domínio / inscrição”*.

12. **Administrador — alerta imediato + relatório semanal legível**  
    Brief Who This Serves.  
    → **Covered** — UJ-3, UJ-4; FR-10–FR-12.

13. **Desenvolvedor — sistema no vídeo, repo documentado, infra desligável**  
    Brief Who This Serves.  
    → **Covered** — UJ-5; SM-2–SM-4; Non-Goals / §10 teardown.

14. **Cliente da API = Postman; sem frontend no MVP**  
    Brief Solution + decisões #2.  
    → **Covered** — Glossário API; §5 Non-Goals portal; §6.1 Postman; §9.

15. **Tom: acadêmico, cost-aware, não comercial; moat = clareza arquitetural**  
    Brief What Makes This Different (*“Não é um produto comercial”*, *“Moat técnico: nenhum”*).  
    → **Covered** (tom acadêmico/custo) em §1/§10; **Qualitative-drop** para *“moat nenhum”* / anti-comercial explícito (irrelevante como requisito de produto).

---

## Escopo funcional (enunciado / brief)

16. **`POST /avaliação` com `{ descricao, nota }` nota 0–10**  
    Brief Solution + Success Criteria; addendum referência PDF.  
    → **Covered** — FR-7; §9; Open Q4 sobre path versionado.

17. **Classificação urgência: ≤4 ALTA (alerta); 5–7 MÉDIA; ≥8 BAIXA; notificação só ALTA**  
    Brief + addendum regras confirmadas.  
    → **Covered** — Glossário Urgência; FR-9, FR-10.

18. **≥2 funções serverless com responsabilidade única (SRP)**  
    Brief Solution + Success Criteria.  
    → **Covered** — FR-10, FR-11; §6.1; SM-1; addendum fluxo.

19. **Notificação: e-mail com descrição, urgência, data**  
    Brief; addendum referência enunciado.  
    → **Covered** — FR-10.

20. **Relatório semanal: média, qty/dia, qty por urgência**  
    Brief Success Criteria + Solution.  
    → **Covered** — FR-11; SM-1.  
    Nota: brief addendum também lista *“descrição, urgência, data”* no relatório (texto do PDF) — PRD foca agregados; detalhe de linha a linha do PDF fica em Open Q5. Tratar como **Covered** no essencial do enunciado (agregados); **Gap** leve se o PDF do enunciado exigir campos por avaliação no relatório além dos agregados — não confirmado no PRD.

21. **Relatório: e-mail HTML (SES) + PDF no S3**  
    Brief Scope IN + decisão #3.  
    → **Covered** — FR-12; §6.1; addendum.

22. **Deploy automatizado dos componentes atualizáveis**  
    Brief Success Criteria.  
    → **Covered** — FR-15; SM-1.

23. **Monitoramento configurado (logs, métricas, alarmes)**  
    Brief Solution + Scope IN CloudWatch.  
    → **Covered** — FR-13–FR-14; NFR Observabilidade; §11.

24. **Sem UI dedicada no MVP (admin/estudante)**  
    Brief Solution + Scope OUT portal.  
    → **Covered** — §5 Non-Goals; §2.2 Non-Users.

---

## Escopo IN técnico (brief / addendum brief)

25. **Java 17 / Spring Boot 3.4.5; Spring Security JWT completo**  
    → **Covered** — addendum PRD Stack; `prd.md` FR-1/FR-2 (capacidades; stack no addendum).

26. **PostgreSQL + Flyway; RDS (ou local + RDS)**  
    → **Covered** — addendum PRD (JPA/PostgreSQL/Flyway); free-tier RDS no brief addendum — ver item 37.

27. **Mensageria: Kafka no Compose local; SQS em produção (não MSK)**  
    → **Covered** — addendum PRD; `prd.md` §5 Non-Goals MSK.

28. **API em ECS Fargate; região sa-east-1**  
    → **Covered** — addendum PRD; `prd.md` §8/§10 região.

29. **Resilience4j (circuit breaker/retry) na API**  
    → **Covered** — addendum PRD; `prd.md` §8 Resiliência (*“retry/circuit conforme addendum”*).

30. **Lambdas notificação + relatório; EventBridge cron semanal**  
    → **Covered** — addendum PRD fluxo; FR-11 assumption cron.

31. **SES (sandbox / e-mail verificado para demo)**  
    → **Covered** — FR-10; §5; §10 Constraints.

32. **Orçamento-alvo ~USD 20–30/mês**  
    → **Covered** — SM-4; §10; addendum Orçamento.

33. **Docker Compose para desenvolvimento local**  
    → **Covered** — addendum PRD Stack.

34. **CI/CD GitHub Actions → deploy AWS (+ ECR)**  
    → **Covered** — addendum PRD; FR-15.

35. **Secrets Manager ou SSM Parameter Store**  
    → **Covered** — addendum PRD Secrets; §8 *“segredos fora do código”*.

36. **Teardown pós-entrega (parar RDS, ECS zero, EventBridge off) — script ou checklist**  
    Brief addendum Economia pós-entrega.  
    → **Covered** (checklist) — SM-4 *“checklist de teardown”*; addendum PRD Orçamento.  
    → **Gap** (leve) — brief admite **script** além de checklist; PRD/addendum não exigem script automatizado de teardown.

37. **Arquitetura cost-aware / free tier onde possível**  
    Brief What Makes This Different + proposta AWS (*“RDS… free tier”*).  
    → **Gap** (leve) — custo e teardown estão Covered; **“free tier” como estratégia explícita** não aparece no PRD nem no addendum do PRD.

38. **Containers mínimos / ECS 0.25 vCPU**  
    Brief cost-aware; addendum brief.  
    → **Covered** — addendum PRD *“ECS Fargate (0.25 vCPU)”*.

---

## Escopo OUT (brief)

39. **Portal web admin/estudante — OUT**  
    → **Covered** — `prd.md` §5, §6.2.

40. **MSK em produção — OUT**  
    → **Covered** — §5; addendum Kafka só local.

41. **Multi-tenant / múltiplos cursos — OUT**  
    Brief Scope OUT.  
    → **Superseded** — addendum PRD *“Mudança vs Product Brief”*: catálogo com N Cursos/Aulas **no mesmo tenant de demo** está **IN**; multi-tenant institucional permanece OUT (`prd.md` §5).

42. **HA / escala além do demo — OUT**  
    → **Covered** — §5; SM-C1.

43. **Cloud 24/7 após entrega — OUT; infra desligável**  
    → **Covered** — §5, §10; SM-4.

---

## Expansões documentadas (brief implícito → PRD)

44. **Catálogo Curso/Aula + inscrição do Estudante**  
    → **Superseded** (brief genérico → PRD expandido) — documentado em addendum PRD tabela “Mudança vs Product Brief”; `prd.md` §4.2–4.3.

45. **JWT com papéis explícitos Estudante + Administrador**  
    → **Superseded** / expandido — addendum PRD; FR-1–FR-2.

46. **Endpoints de listagem/consulta IN**  
    → **Superseded** / expandido — addendum PRD; FR-4, FR-8.

47. **Demo: health check + métricas de mercado + PDF S3 + roteiro YouTube**  
    Brief tinha vídeo/monitoramento genéricos.  
    → **Covered** (expansão consciente) — addendum PRD linha Demo; FR-13–FR-16; §11.

---

## Diferenciação / qualitativos menores

48. **Separação clara de responsabilidades como critério de avaliação do desafio**  
    Brief What Makes This Different.  
    → **Covered** — linguagem SRP em FR-10/FR-11; SM-C2.

49. **Restrições extras: nenhuma além do PDF**  
    Brief addendum Contexto.  
    → **Qualitative-drop** — meta-afirmação de processo; não precisa virar requisito do PRD (Open Questions cobrem ambiguidades).

50. **Memlog: fast path; assumptions urgência/SQS/SES/ECS**  
    → **Covered** — refletidas no PRD + addendum.

---

## Resumo de Gaps

| # | Gap | Severidade |
|---|-----|------------|
| G1 | Critério *“modelo cloud escolhido”* explicado na entrega não vira SM/cena explícita | Média |
| G2 | Restrição operacional **solo / sem grupo** ausente do PRD | Baixa–média |
| G3 | **Free tier** como âncora de custo não reafirmado no addendum do PRD | Baixa |
| G4 | **Script** de teardown (além de checklist) não exigido | Baixa |
| G5 | Campos *descrição/urgência/data* no relatório (texto PDF do enunciado no brief addendum) vs só agregados no FR-11 — ambiguidade residual (Open Q5 ajuda) | Baixa |

Itens **Superseded** principais já documentados no `addendum.md` do PRD (Curso/Aula, múltiplos cursos no tenant, papéis JWT, listagens, demo expandida).
