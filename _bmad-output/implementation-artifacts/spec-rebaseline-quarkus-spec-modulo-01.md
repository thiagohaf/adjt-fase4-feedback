---
title: 'Re-baseline Quarkus — spec do módulo 01 (Passo 3)'
type: 'chore'
created: '2026-07-21'
status: 'done'
review_loop_iteration: 0
route: 'one-shot'
context: []
---

# Re-baseline Quarkus — spec do módulo 01 (Passo 3)

## Intent

**Problem:** A spec OpenSpec do módulo 01 (Autenticação e Papéis) ainda referenciava a stack Spring Boot 4.0.7 / Spring Security, contradizendo o Architecture Spine re-baselineado para Quarkus 3.33 LTS pela Sprint Change Proposal aprovada em 2026-07-21 (Passo 3, §4).

**Approach:** Atualizar `design.md` (stack, diagrama, estrutura de pacotes, §7 segurança, exceções, config, testes, pom de referência, decisão de biblioteca JWT) e `proposal.md` (mecanismo de autorização) para Quarkus Security + SmallRye JWT (HS256 mantido), preservando intactos os invariantes: matriz de autorização, contratos HTTP `/api/v1/`, códigos de erro `{code, message, traceId}`, Flyway V1/V2, papéis ESTUDANTE/ADMINISTRADOR, chaves de secrets. `specs.md` não muda (agnóstico a mecanismo).

## Suggested Review Order

1. [design.md — linha de stack e visão arquitetural (§1)](../../openspec/modules/01-autenticacao-e-papeis/design.md) — diagrama e fluxos agora via `SecurityIdentity`/extensão SmallRye JWT.
2. [design.md §7 — Quarkus Security + SmallRye JWT](../../openspec/modules/01-autenticacao-e-papeis/design.md) — config HS256 (JWK simétrico, `smallrye.jwt.path.groups=role`, `quarkus.http.auth.proactive=false`) e rotas `@RolesAllowed`.
3. [design.md §8–§9 — ExceptionMappers e config](../../openspec/modules/01-autenticacao-e-papeis/design.md) — preservação dos códigos de erro do contrato e fonte única de secret.
4. [design.md §12 — pom de referência Quarkus](../../openspec/modules/01-autenticacao-e-papeis/design.md) — BOM `quarkus-bom` + extensões (inclui BCrypt via `quarkus-elytron-security-common`).
5. [proposal.md — mecanismo de autorização](../../openspec/modules/01-autenticacao-e-papeis/proposal.md) — "(Quarkus Security)", "`@RolesAllowed` server-side", correção V1/V2 do seed.
6. [review-findings-passo3.md](review-findings-passo3.md) — relatório da revisão adversarial (16 findings; 4 HIGH corrigidos).
