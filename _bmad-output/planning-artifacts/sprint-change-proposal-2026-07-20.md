---
title: "Sprint Change Proposal — Troca de stack: Spring Boot → Quarkus"
status: superseded
superseded-by: "_bmad-output/planning-artifacts/sprint-change-proposal-2026-07-21.md"
created: 2026-07-20
project: adjt-fase4
author: ThiagoFerreira (via bmad-correct-course)
trigger: "Decisão de trocar o framework da API e das Lambdas de Spring Boot 4.0.7 para Quarkus"
scope-classification: MAJOR
inputs:
  - "_bmad-output/planning-artifacts/prds/prd-adjt-fase4-2026-07-20/"
  - "_bmad-output/planning-artifacts/architecture/architecture-adjt-fase4-2026-07-20/"
  - "_bmad-output/planning-artifacts/briefs/brief-adjt-fase4-2026-07-19/"
  - "openspec/modules/01-autenticacao-e-papeis/"
  - "feedbacks/apps/api/ (código Spring Boot já implementado)"
---

# Sprint Change Proposal — Spring Boot → Quarkus

## 1. Resumo do problema (Issue Summary)

**Gatilho:** decisão de trocar o framework de toda a parte Java do projeto — API monolítica e as duas Lambdas — de **Spring Boot 4.0.7** para **Quarkus** (LTS atual: **3.33**, suporte até 25/03/2027).

**Contexto da descoberta:** a troca foi levantada após o módulo 01 (Autenticação e Papéis) já estar implementado e testado em Spring Boot, com a arquitetura (Architecture Spine) inteiramente amarrada ao ecossistema Spring — inclusive por decisão adotada explícita (AD-13: Lambdas via Spring Cloud Function).

**Evidências do acoplamento atual à stack Spring:**

- `feedbacks/apps/api/pom.xml`: parent `spring-boot-starter-parent 4.0.7`; starters Web, Security, Data JPA, Validation, Actuator.
- Código do módulo 01: `SecurityConfig`, `JwtAuthenticationFilter`, `SpringCurrentUserProvider`, repositórios Spring Data JPA, testes com `spring-boot-starter-test`/`spring-security-test`/MockMvc.
- Architecture Spine: AD-13 (`[ADOPTED]`) exige Spring Boot + Spring Cloud Function nas Lambdas; tabela Stack fixa Boot 4.0.7, Spring Cloud BOM, Resilience4j `spring-boot4`; convenções de teste citam o stack Spring.
- Addendums do brief e do PRD: tabelas de stack citando "Java 17, Spring Boot 3.4.5" e Spring Security.
- Spec do módulo 01 (`openspec/modules/01-autenticacao-e-papeis/`): referências a Spring Security na matriz de autorização e no design.

**Categoria do problema (checklist 1.2):** pivô estratégico de tecnologia — não é limitação técnica descoberta nem requisito novo de stakeholder. Os 17 FRs do PRD permanecem intactos.

## 2. Análise de impacto (Impact Analysis)

### 2.1 Impacto no backlog (módulos openspec — não há épicos formais nem sprint-status.yaml)

| Módulo | Estado | Impacto |
| --- | --- | --- |
| 01 Autenticação e Papéis | **Implementado em Spring** | **Retrabalho de código e testes** (maior custo da mudança). Spec `design.md` precisa trocar referências Spring Security → Quarkus Security/SmallRye JWT. `proposal.md`/`specs.md`: impacto mínimo (comportamento não muda). |
| 02+ Catálogo, Inscrição, Avaliação, Alerta, Relatórios | Não iniciados (controllers esqueléticos) | Nenhum retrabalho: nascem já no padrão Quarkus **depois** que a fundação for migrada. Ordem/prioridade dos módulos não muda. |

Nenhum módulo é invalidado; nenhum módulo novo é necessário. O que muda é a **fundação** sobre a qual todos serão construídos.

### 2.2 PRD

- **Corpo do PRD: sem conflito.** Ele é contrato de capacidades; nenhum FR, UJ, non-goal ou métrica de sucesso cita Spring. MVP permanece o mesmo.
- **Ajustes pontuais:** `addendum.md` do PRD (tabela Stack: linha "Java 17, Spring Boot 3.4.5" e "Spring Security, JWT completo") e roteiro §11 cena 1–2 (slide da stack e narrativa do "porquê" — Quarkus fortalece o argumento serverless: startup rápido, footprint menor em Lambda).
- `addendum.md` do brief: mesma tabela de stack desatualizada (registro histórico — atualizar ou anotar supersedência).

### 2.3 Arquitetura (artefato mais impactado)

O **paradigma se mantém** (Modular Monolith Layered + Async Side Effects), assim como toda a topologia AWS (ECS Fargate + ALB, RDS, SQS+DLQ, EventBridge, SES, S3, CloudWatch, CDK, sa-east-1). ADs de domínio (AD-3, AD-4, AD-5, AD-6, AD-15, AD-16) e de infra AWS (AD-1 exceto menção "Spring Boot", AD-7, AD-10, AD-12, AD-17) permanecem válidos. Precisam de revisão:

| Item | Hoje | Com Quarkus |
| --- | --- | --- |
| AD-1 (texto) | "um deployable **Spring Boot** em ECS Fargate" | "um deployable **Quarkus** em ECS Fargate" (regra inalterada no resto) |
| AD-2 | "wiring Spring não cria dependência" | Mesma regra com CDI (ArC); direção `api → application → domain ← infrastructure` inalterada |
| AD-8 | Spring Security resource-server/filter, JWT HS256 | Quarkus Security + **SmallRye JWT** (`quarkus-smallrye-jwt`); emissão via Build/`io.smallrye.jwt.build`. Atenção: SmallRye JWT privilegia RSA — manter HS256 exige config explícita (`mp.jwt.verify.publickey.algorithm=HS256` + secret) ou migrar a decisão para RS256. Matriz de papéis inalterada. |
| **AD-13** | Spring Boot + Spring Cloud Function + `FunctionInvoker` | **Reescrever a decisão**: `quarkus-amazon-lambda` (handler nativo Quarkus); sem servidor HTTP embutido nas Lambdas — mesmo espírito da regra atual |
| AD-11 | Actuator health | `quarkus-smallrye-health` (`/q/health`) com checagem de DB; métricas continuam no CloudWatch/ALB |
| AD-14 | JaCoCo no build Spring | Mantém-se: `quarkus-jacoco` + gate 90% no Maven |
| AD-18 | Resilience4j `spring-boot4` | **SmallRye Fault Tolerance** (`@Retry`, `@CircuitBreaker` — MicroProfile); DLQ/retry SQS inalterados |
| Tabela Stack | Boot 4.0.7, Spring Cloud BOM/SCF, Resilience4j | Quarkus **3.33 LTS** (BOM), quarkus-rest + jackson, quarkus-hibernate-orm (ou Panache), quarkus-flyway, quarkus-smallrye-jwt, quarkus-smallrye-health, quarkus-amazon-lambda, quarkus-messaging-kafka (local) / SQS SDK v2, smallrye-fault-tolerance, quarkus-jacoco |
| Consistency Conventions | "Testes: JUnit 5 + Mockito" (stack Spring implícito) | `@QuarkusTest` + RestAssured para web; JUnit 5 + Mockito seguem nos unitários puros |
| Starter | Spring Initializr | `code.quarkus.io` / CLI `quarkus create` |

**O que NÃO muda:** contratos HTTP (AD-9, `/api/v1/`), contrato do evento de alerta (AD-5), janelas de relatório (AD-6), modelo de erros `{code, message, traceId}`, schema Flyway (V1/V2 são SQL puro — reaproveitados como estão), ER diagram, S3 keys, chaves de secrets, envelope de VPC.

### 2.4 UI/UX

N/A — projeto sem UI (Postman + e-mail).

### 2.5 Artefatos secundários / técnicos

| Artefato | Impacto |
| --- | --- |
| Código `feedbacks/apps/api` | **Reescrita da casca**: `pom.xml` (BOM Quarkus), segurança (SecurityConfig/filtros → SmallRye JWT + anotações `@RolesAllowed`), adapters JPA (Spring Data → Hibernate ORM/Panache), health (Actuator → SmallRye Health), `application.yml` → `application.properties` com profiles `%local`/`%aws`. **Domain e application (casos de uso, portas) sobrevivem quase intactos** — o desenho hexagonal AD-2 paga dividendo aqui. |
| Testes do módulo 01 | Reescrever camada web: MockMvc → `@QuarkusTest` + RestAssured. Testes unitários de use case (Mockito) sobrevivem. |
| CI/CD (GitHub Actions) | Ajustar build (`quarkus:build`), empacotamento fast-jar/uber-jar, JaCoCo via `quarkus-jacoco`. |
| Dockerfile / imagem ECS | Usar `Dockerfile.jvm` padrão Quarkus (build nativo fica **fora do MVP** — custo/tempo). |
| CDK / infra | Praticamente inalterado: ECS/ALB/RDS/SQS/EventBridge/SES/S3 são agnósticos. Lambdas continuam runtime `java17`; muda só o handler configurado. |
| docker-compose local | Inalterado (Postgres + Kafka). |
| Postman collection / roteiro do vídeo | Endpoints inalterados; atualizar slide de stack e a justificativa do modelo cloud (cena 2). |

## 3. Abordagem recomendada (Recommended Approach)

### Opções avaliadas (checklist §4)

| Opção | Avaliação | Esforço | Risco |
| --- | --- | --- | --- |
| **1. Direct Adjustment** — re-baseline da arquitetura + migração do módulo 01 + módulos seguintes já em Quarkus | **Viável e recomendada** (se a troca for confirmada) | Médio-alto: ~1,5–2,5 dias para fundação + módulo 01 migrado com testes e CI verdes | **Alto por causa do prazo**: consome ~20–25% dos 8 dias restantes sem entregar FR novo |
| 2. Rollback | N/A — não há o que reverter; o equivalente é **desistir da troca** e seguir em Spring (custo zero de retrabalho) | — | — |
| 3. MVP Review | Não necessário — o MVP continua o mesmo em qualquer framework; nenhum FR sai do escopo por causa da troca | — | — |

### Recomendação

**Direct Adjustment**, com uma condição de honestidade registrada: a troca **não é exigida pelo enunciado** (o Tech Challenge pede cloud + serverless + segurança + deploy + monitoramento, não um framework específico) e o prazo é 28/07. Vale a pena se o motivo for aprendizado/justificativa técnica no vídeo (cold start menor em Lambda é um argumento real e filmável). Se o motivo for fraco, a alternativa racional é permanecer em Spring. **Go/no-go é a primeira decisão desta proposta.**

Mitigadores que tornam a migração viável no prazo:

1. Arquitetura hexagonal (AD-2) isola o retrabalho na casca: domain/application do módulo 01 migram quase sem tocar.
2. Migrações Flyway, contratos HTTP, contrato de evento e toda a infra AWS/CDK são reaproveitados como estão.
3. Só o módulo 01 foi implementado — o custo de troca **nunca será menor do que agora**.

## 4. Propostas de mudança detalhadas (o que refazer, **em ordem**)

> Ordem de execução obrigatória — cada passo desbloqueia o seguinte. Estimativas para dev solo.

### Passo 0 — Go/no-go (decisão, 0h)

Confirmar que o benefício (aprendizado + narrativa serverless no vídeo) compensa ~2 dias de retrabalho a 8 dias da entrega. **Sem essa confirmação, nada abaixo é executado.**

### Passo 1 — Re-baseline da Arquitetura (0,5 dia) — `ARCHITECTURE-SPINE.md`

- **AD-13** (mudança central):

  ```
  ANTES (AD-13): cada Lambda é app Spring Boot + Spring Cloud Function
  (spring-cloud-function-adapter-aws); handler FunctionInvoker; sem starter Web/Tomcat.

  DEPOIS (AD-13): cada Lambda é app Quarkus + quarkus-amazon-lambda;
  handler gerenciado pelo Quarkus; sem servidor HTTP embutido; packaging
  function.zip padrão Quarkus.

  Racional: SCF é específico do ecossistema Spring; quarkus-amazon-lambda
  é o adaptador nativo equivalente, com startup menor.
  ```

- **AD-8**: trocar "Spring Security resource-server/filter" por "Quarkus Security + SmallRye JWT"; **decidir explicitamente** manter HS256 (config `mp.jwt.verify.publickey.algorithm=HS256`, secret continua em Secrets Manager com chave `jwtSecret`) ou migrar para RS256 — recomendação: **manter HS256** para não tocar AD-12 nem o seed de secrets.
- **AD-18**: Resilience4j → SmallRye Fault Tolerance (mesma regra, anotações MicroProfile).
- **AD-11**: Actuator → SmallRye Health (`/q/health` com checagem de DB); expor no path compatível com o ALB health check.
- **AD-1/AD-2**: ajustes de texto ("Spring Boot" → "Quarkus"; "wiring Spring" → "CDI/ArC").
- **Tabela Stack + Starter + Consistency Conventions (linha Testes, linha Config)**: nova stack Quarkus 3.33 LTS (ver §2.3); `application.properties` com profiles `%local`/`%aws`.

### Passo 2 — Addendums de PRD e brief (0,5h)

- `prds/prd-adjt-fase4-2026-07-20/addendum.md` — tabela Stack:

  ```
  ANTES: Linguagem / Framework | Java 17, Spring Boot 3.4.5
         Segurança             | Spring Security, JWT completo
         Persistência          | Spring Data JPA, PostgreSQL, Flyway
         Resiliência           | Resilience4j

  DEPOIS: Linguagem / Framework | Java 17, Quarkus 3.33 LTS
          Segurança             | Quarkus Security + SmallRye JWT (HS256)
          Persistência          | Hibernate ORM (Panache), PostgreSQL, Flyway
          Resiliência           | SmallRye Fault Tolerance
  ```

- `briefs/brief-adjt-fase4-2026-07-19/addendum.md` — mesma tabela; adicionar nota "superseded pela Sprint Change Proposal 2026-07-20" (preservar histórico da decisão original).
- PRD corpo: **nenhuma mudança** (nenhum FR/UJ cita framework).

### Passo 3 — Spec do módulo 01 (0,5h) — `openspec/modules/01-autenticacao-e-papeis/`

- `design.md`: trocar referências de mecanismo (Spring Security filter chain → SmallRye JWT + `@RolesAllowed`/`SecurityIdentity`); manter matriz de autorização, contratos e erros como estão.
- `proposal.md` §3.1 item 3: "enforced server-side (Spring Security)" → "(Quarkus Security)".
- `specs.md`: revisar apenas se citar mecanismo — comportamento observável (401/403, claims, seed) não muda.

### Passo 4 — Migração do código do módulo 01 (1–1,5 dia) — `feedbacks/apps/api`

Ordem interna sugerida:

1. `pom.xml`: BOM `quarkus-bom` 3.33 LTS; extensões `quarkus-rest-jackson`, `quarkus-hibernate-orm`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-smallrye-jwt` (+ `smallrye-jwt-build`), `quarkus-hibernate-validator`, `quarkus-smallrye-health`, `quarkus-jacoco`, `quarkus-junit5` + RestAssured; plugin `quarkus-maven-plugin`; gate JaCoCo 90% preservado.
2. Config: `application.yml`/`application-aws.yml` → `application.properties` com `%local`/`%aws` (ou extensão `quarkus-config-yaml` para minimizar diff).
3. Segurança: remover `SecurityConfig`/`JwtAuthenticationFilter`/`JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler`; emitir JWT com `smallrye-jwt-build` no `JwtTokenService`; proteger rotas com `@RolesAllowed`; `SpringCurrentUserProvider` → provider baseado em `SecurityIdentity` (porta `CurrentUserProvider` **não muda**).
4. Persistência: repositórios Spring Data → Panache/`EntityManager` implementando as **mesmas portas** (`UsuarioRepository`, `AvaliacaoReadRepository`); entidades JPA e migrações Flyway V1/V2 inalteradas.
5. Web: anotações Spring MVC → JAX-RS nos controllers (`@RestController` → `@Path`; `GlobalExceptionHandler` → `ExceptionMapper`s) mantendo o corpo de erro `{code, message, traceId}` e o `TraceIdFilter` (→ `ContainerRequestFilter`).
6. Health: `HealthController`/Actuator → SmallRye Health com check de DB.
7. Testes: use cases (Mockito) mantidos; camada web reescrita com `@QuarkusTest` + RestAssured; helpers de JWT ajustados. **Critério de saída: suíte verde + JaCoCo ≥ 90%.**

### Passo 5 — CI/CD e empacotamento (0,5 dia)

- GitHub Actions: build `mvn verify` (JaCoCo via `quarkus-jacoco`), empacotar fast-jar, `Dockerfile.jvm` → ECR.
- Build nativo GraalVM: **registrar como Deferred** (custo de build alto; cold start em Lambda tolerado pelo NFR do PRD).

### Passo 6 — Módulos seguintes já em Quarkus (sem retrabalho)

- Módulos 02+ (catálogo, inscrição, avaliação, publish SQS/Kafka) seguem a nova fundação: `quarkus-messaging-kafka` no profile `%local`, SQS SDK v2 no `%aws`, porta `EvaluationEventPublisher` inalterada (AD-5/AD-7 preservados).
- Lambdas (`notification`, `report`): criar com `quarkus-amazon-lambda` conforme novo AD-13; OpenPDF continua na `lambda-report`.

### Passo 7 — Roteiro do vídeo (0,5h)

- Cena 1–2: atualizar slide da stack; incorporar justificativa "Quarkus para startup rápido/baixo footprint em container e Lambda" — fortalece o requisito de explicar o modelo cloud (SM-2).

**Esforço total estimado: ~2 a 2,5 dias** de um total de 8 até a entrega.

## 5. Handoff de implementação

**Classificação de escopo: MAJOR** — muda decisão adotada de arquitetura (AD-13, AD-8, AD-18) e re-baseline da stack, embora não altere escopo de produto.

| Papel | Responsabilidade |
| --- | --- |
| Architect (skill `bmad-architecture`, intent update) | Passo 1 — re-baseline do ARCHITECTURE-SPINE com os ADs revisados desta proposta |
| PM/Analyst (edição direta) | Passo 2 — addendums de PRD e brief |
| Developer (skill `bmad-quick-dev` ou `bmad-dev-story`) | Passos 3–5 — spec do módulo 01, migração do código, CI |
| Developer (fluxo normal de módulos) | Passo 6 — módulos seguintes na nova fundação |
| Thiago (conteúdo) | Passo 7 — slide/roteiro do vídeo |

**Critérios de sucesso da mudança:**

1. ARCHITECTURE-SPINE atualizado sem referência residual a Spring (exceto histórico/registro da decisão).
2. Módulo 01 migrado: mesma superfície HTTP, mesmos códigos de erro, suíte verde, JaCoCo ≥ 90%.
3. Pipeline CI verde com build Quarkus e imagem no ECR.
4. Nenhum FR do PRD alterado; Postman collection do módulo 01 funciona sem mudança.

## 6. Status do checklist (registro)

| Seção | Status | Nota |
| --- | --- | --- |
| 1. Trigger e contexto | [x] Done | Pivô estratégico de stack; evidências em §1 |
| 2. Impacto em épicos | [x] Done (adaptado) | Sem épicos formais; análise sobre módulos openspec (§2.1). sprint-status.yaml inexistente → N/A |
| 3. Conflitos em artefatos | [x] Done | PRD ok; addendums, arquitetura, spec 01, código, CI mapeados (§2.2–2.5). UX N/A |
| 4. Caminho adiante | [x] Done | Direct Adjustment recomendado com go/no-go explícito (§3) |
| 5. Componentes da proposta | [x] Done | Este documento |
| 6. Revisão final e aprovação | [!] Action-needed | **Aguarda aprovação explícita do usuário** |
