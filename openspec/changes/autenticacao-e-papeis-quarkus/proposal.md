# Proposal — Implementar Módulo 01: Autenticação e Papéis (Quarkus)

> **Fonte de verdade funcional:** `openspec/modules/01-autenticacao-e-papeis/` (proposal.md, specs.md, design.md).
> Esta change **deriva** desses documentos — não os reinventa. Restrições arquiteturais em
> `_bmad-output/planning-artifacts/architecture/architecture-adjt-fase4-2026-07-20/ARCHITECTURE-SPINE.md`
> (AD-2, AD-8, AD-9, AD-12, AD-14) — não editar.

## Why

A Plataforma de Feedbacks FIAP expõe uma API REST sem UI (interação via Postman) e hoje não possui
nenhuma linha de código de aplicação: sem autenticação server-side não há como distinguir
Estudante de Administrador, proteger rotas de negócio nem executar as User Journeys UJ-1/UJ-2.
O módulo 01 é fundacional — bloqueia Catálogo, Inscrição, Avaliação e listagens (módulos 02+).
O enunciado e o PRD (FR-1, FR-2) exigem JWT completo: emissão no login, validação em rotas
protegidas e papéis enforced server-side.

## What Changes

- Cria a aplicação Quarkus 3.33 LTS em `feedbacks/apps/api/` (Java 17, Maven, BOM `quarkus-bom`),
  estrutura hexagonal `com.fiap.feedbacks.{api,application,domain,infrastructure}` conforme AD-2 e
  design.md §2 do módulo 01.
- Implementa `POST /api/v1/auth/login` público que valida credenciais (BCrypt) e emite JWT HS256
  com claims `sub` (UUID), `role`, `iat`, `exp` (TTL configurável, default 24h) via
  `quarkus-smallrye-jwt-build`.
- Implementa validação de token nas rotas protegidas via `quarkus-smallrye-jwt` + Quarkus Security:
  `mp.jwt.verify.publickey.algorithm=HS256`, chave simétrica em formato JWK
  (`smallrye.jwt.verify.secretkey` derivada de `app.jwt.secret` — fonte única `jwtSecret`),
  `smallrye.jwt.path.groups=role`, `quarkus.http.auth.proactive=false`, sem verificação de `iss`.
- Implementa autorização por papel (`@RolesAllowed` com `ESTUDANTE` | `ADMINISTRADOR`) segundo a
  matriz do proposal.md §3.3 do módulo 01 (AD-8).
- Persiste `Usuario` (Hibernate ORM + PostgreSQL) com migrações Flyway `V1__create_schema.sql`
  (schema) e `V2__seed_usuarios.sql` (seed demo: `estudante@demo.fiap` / `admin@demo.fiap`,
  senhas com hash BCrypt).
- Implementa envelope de erro `{ code, message, traceId }` via `ExceptionMapper`s JAX-RS +
  `TraceIdFilter` (MDC), cobrindo o mapa de códigos do specs.md do módulo 01:
  400 `VALIDATION_ERROR`; 401 `AUTH_INVALID_CREDENTIALS` / `AUTH_MISSING_TOKEN` /
  `AUTH_INVALID_TOKEN` / `AUTH_TOKEN_EXPIRED`; 403 `AUTH_FORBIDDEN`.
- Configuração por profiles `%local` / `%aws` (AD-12: secret `jwtSecret` via env local /
  Secrets Manager em AWS; nenhum segredo de produção hardcoded).
- Testes: JUnit 5 + Mockito nos use cases/token service; `@QuarkusTest` + RestAssured na camada
  web; **todo cenário de specs.md do módulo 01 coberto por teste**; JaCoCo line coverage ≥ 90%
  (AD-14).
- Reaproveitamento seletivo do branch de referência Spring
  (`feature/openspec-01-autenticacao-e-papeis-implementacao`, **não mergear**): `domain/`,
  `application/` e migrações Flyway V1/V2 são agnósticos de framework — copiar validando contra o
  design.md do módulo 01.

## Capabilities

### New Capabilities

- `auth-login`: login com credenciais (email/senha BCrypt) e emissão de JWT HS256 com claim
  `role`; validação de payload; códigos de erro 400/401 (FR-1; SPEC-1.1–1.6).
- `auth-token-validation`: validação de Bearer token nas rotas protegidas (assinatura HS256,
  expiração); rotas públicas login/health; 401 com envelope padronizado e distinção
  `AUTH_MISSING_TOKEN` / `AUTH_INVALID_TOKEN` / `AUTH_TOKEN_EXPIRED` (FR-1; SPEC-1.7–1.11).
- `auth-role-authorization`: autorização por papel server-side (`@RolesAllowed`) segundo a matriz
  AD-8/§3.3, incluindo porta `CurrentUserProvider` para regras finas downstream; 403
  `AUTH_FORBIDDEN` (FR-2; SPEC-2.x cobertos na extensão implementável neste módulo).

### Modified Capabilities

<!-- Nenhuma — não há specs pré-existentes em openspec/specs/. -->

## Impact

- **Código novo:** `feedbacks/apps/api/` (pom.xml, código de produção nas 4 camadas, migrações
  Flyway, `application.properties`, suíte de testes). Nenhum código existente é alterado.
- **Branch:** `feature/openspec-01-autenticacao-e-papeis-quarkus` a partir do `develop`;
  PR de saída para `develop`. Limpar resíduo untracked `target/` se existir.
- **Dependências:** extensões Quarkus (rest-jackson, hibernate-orm, jdbc-postgresql, flyway,
  smallrye-jwt, smallrye-jwt-build, hibernate-validator, smallrye-health,
  elytron-security-common p/ BCrypt; junit5, rest-assured, jacoco em teste) — design.md §12.
  **Nenhuma dependência Spring.**
- **Rotas de módulos futuros:** SPEC-2.x referencia rotas de Curso/Aula/Avaliação/Inscrição que
  nascem nos módulos 02+; neste módulo a matriz é verificada com recursos mínimos/stub apenas o
  suficiente para provar a autorização (detalhado em design/specs da change).
- **Divergência potencial a registrar:** se `mp.jwt.verify.publickey.algorithm=HS256` se provar
  inviável e for necessário `smallrye.jwt.verify.algorithm=HS256`, seguir em frente e registrar a
  divergência nesta change para posterior ajuste do design/Spine.
- **Fora de escopo:** CI/CD (GitHub Actions, Dockerfile), módulos 02+ (implementação de negócio),
  build nativo GraalVM, refresh token, signup, MFA.
