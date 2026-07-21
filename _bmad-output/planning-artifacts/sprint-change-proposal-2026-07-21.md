---
title: "Sprint Change Proposal — Troca de stack: Spring Boot → Quarkus"
status: aprovado
approved: 2026-07-21 (ThiagoFerreira)
created: 2026-07-21
project: adjt-fase4
author: ThiagoFerreira (via bmad-correct-course)
trigger: "Decisão de trocar o framework da API e das Lambdas de Spring Boot 4.0.7 para Quarkus"
scope-classification: MAJOR
supersedes: "_bmad-output/planning-artifacts/sprint-change-proposal-2026-07-20.md"
inputs:
  - "_bmad-output/planning-artifacts/prds/prd-adjt-fase4-2026-07-20/"
  - "_bmad-output/planning-artifacts/architecture/architecture-adjt-fase4-2026-07-20/"
  - "_bmad-output/planning-artifacts/briefs/brief-adjt-fase4-2026-07-19/"
  - "openspec/modules/01-autenticacao-e-papeis/"
  - "branch feature/openspec-01-autenticacao-e-papeis-implementacao (código Spring, NÃO mergeado)"
---

# Sprint Change Proposal — Spring Boot → Quarkus (v2, 2026-07-21)

> **Supersede** o rascunho de 2026-07-20 (nunca aprovado). Diferenças principais desta versão:
> (a) constatação de que o código Spring do módulo 01 **não está mergeado** no develop — vive só no
> branch `feature/openspec-01-autenticacao-e-papeis-implementacao`; (b) a recomendação muda de
> "migrar o código Spring" para "**reimplementar em Quarkus a partir da spec**, com reaproveitamento
> seletivo de domain/application"; (c) prazo recalculado: **7 dias** até 28/07.

## 1. Resumo do problema (Issue Summary)

**Gatilho:** decisão de trocar o framework de toda a parte Java do projeto — API monolítica e as duas
Lambdas — de **Spring Boot 4.0.7** para **Quarkus** (LTS atual: **3.33**, suporte até 25/03/2027).

**Contexto da descoberta:** a troca foi levantada após o módulo 01 (Autenticação e Papéis) já estar
implementado e testado em Spring Boot (94,5% de cobertura), com o Architecture Spine inteiramente
amarrado ao ecossistema Spring — inclusive por decisão adotada explícita (AD-13: Lambdas via Spring
Cloud Function). O branch de trabalho atual (`feature/replanejamento-quarkus`) já foi criado para
formalizar o replanejamento.

**Estado real do código (verificado em 2026-07-21):**

- Branch atual `feature/replanejamento-quarkus` (a partir do develop): contém **apenas** specs e
  artefatos de planejamento — nenhum código-fonte Java. O diretório `feedbacks/apps/api/` no working
  tree contém só `target/` (resíduo de build, untracked); **não há `src/` nem `pom.xml` versionados
  neste branch**.
- Branch `feature/openspec-01-autenticacao-e-papeis-implementacao` (2 commits, **não mergeado**):
  implementação Spring completa do módulo 01 — 57 arquivos, ~2.209 linhas, incluindo `pom.xml`
  (parent `spring-boot-starter-parent 4.0.7`), `SecurityConfig`, `JwtAuthenticationFilter`,
  `SpringCurrentUserProvider`, repositórios Spring Data JPA, migrações Flyway V1/V2, testes
  MockMvc/`spring-security-test` e workflow GitHub Actions com gate JaCoCo.

**Evidências do acoplamento à stack Spring nos artefatos:**

- Architecture Spine: AD-13 `[ADOPTED]` exige Spring Boot + Spring Cloud Function nas Lambdas;
  AD-8 cita Spring Security; AD-11 cita Actuator; AD-18 cita Resilience4j `spring-boot4`; tabela
  Stack fixa Boot 4.0.7 + Spring Cloud BOM; convenções e Structural Seed citam o stack Spring.
- Addendums do brief e do PRD: tabelas de stack com "Java 17, Spring Boot 3.4.5" e Spring Security.
- Spec do módulo 01 (`openspec/modules/01-autenticacao-e-papeis/`): `design.md` referencia Spring
  Boot 4.0.7/Spring Security no stack, diagrama e pom de exemplo; `proposal.md` cita Spring Security
  na matriz de autorização.

**Categoria do problema (checklist 1.2):** pivô estratégico de tecnologia — não é limitação técnica
descoberta nem requisito novo de stakeholder. Os 17 FRs do PRD permanecem intactos.

## 2. Análise de impacto (Impact Analysis)

### 2.1 Impacto no backlog (módulos openspec — não há épicos formais nem sprint-status.yaml)

| Módulo | Estado | Impacto |
| --- | --- | --- |
| 01 Autenticação e Papéis | Spec no develop; **código Spring só em branch não-mergeado** | **Reimplementação em Quarkus** (maior custo da mudança), com reaproveitamento de domain/application. Spec `design.md` precisa trocar referências Spring Security → Quarkus Security/SmallRye JWT. `proposal.md`/`specs.md`: impacto mínimo (comportamento não muda). |
| 02+ Catálogo, Inscrição, Avaliação, Alerta, Relatórios | Não iniciados | Nenhum retrabalho: nascem já no padrão Quarkus **depois** que a fundação existir. Ordem/prioridade dos módulos não muda. |

Nenhum módulo é invalidado; nenhum módulo novo é necessário. O que muda é a **fundação** sobre a
qual todos serão construídos — e o fato de o código Spring não estar mergeado significa que o
develop **nunca precisa conter código Spring**: a linha do tempo do repositório fica limpa.

### 2.2 PRD

- **Corpo do PRD: sem conflito.** É contrato de capacidades; nenhum FR, UJ, non-goal ou métrica de
  sucesso cita Spring. MVP permanece o mesmo.
- **Ajustes pontuais:** `addendum.md` do PRD (tabela Stack: linhas "Java 17, Spring Boot 3.4.5",
  "Spring Security, JWT completo", "Spring Data JPA", "Resilience4j") e roteiro §11 cenas 1–2
  (slide da stack e narrativa do "porquê" — Quarkus fortalece o argumento serverless: startup
  rápido, footprint menor em Lambda).
- `addendum.md` do brief: mesma tabela de stack desatualizada — anotar supersedência (registro
  histórico da decisão original).

### 2.3 Arquitetura (artefato mais impactado)

O **paradigma se mantém** (Modular Monolith Layered + Async Side Effects), assim como toda a
topologia AWS (ECS Fargate + ALB, RDS, SQS+DLQ, EventBridge, SES, S3, CloudWatch, CDK, sa-east-1).
ADs de domínio (AD-3, AD-4, AD-5, AD-6, AD-15, AD-16) e de infra AWS (AD-7, AD-9, AD-10, AD-12,
AD-17) permanecem válidos como estão. Precisam de revisão:

| Item | Hoje | Com Quarkus |
| --- | --- | --- |
| AD-1 (texto) | "um deployable **Spring Boot** em ECS Fargate" | "um deployable **Quarkus** em ECS Fargate" (regra inalterada no resto) |
| AD-2 | "wiring Spring não cria dependência" | Mesma regra com CDI (ArC); direção `api → application → domain ← infrastructure` inalterada |
| AD-8 | Spring Security resource-server/filter, JWT HS256 | Quarkus Security + **SmallRye JWT** (`quarkus-smallrye-jwt`); emissão via `smallrye-jwt-build`. Atenção: SmallRye JWT privilegia RSA — manter HS256 exige config explícita (`mp.jwt.verify.publickey.algorithm=HS256` + secret). Recomendação: **manter HS256** para não tocar AD-12 nem as chaves de secrets. Matriz de papéis inalterada. |
| **AD-13** | Spring Boot + Spring Cloud Function + `FunctionInvoker` | **Reescrever a decisão**: `quarkus-amazon-lambda` (handler nativo Quarkus); sem servidor HTTP embutido nas Lambdas — mesmo espírito da regra atual |
| AD-11 | Actuator health | `quarkus-smallrye-health` (`/q/health`) com checagem de DB; métricas continuam no CloudWatch/ALB |
| AD-14 | JaCoCo no build Spring | Mantém-se: `quarkus-jacoco` + gate 90% no Maven |
| AD-18 | Resilience4j `spring-boot4` | **SmallRye Fault Tolerance** (`@Retry`, `@CircuitBreaker` — MicroProfile); DLQ/retry SQS inalterados |
| Tabela Stack | Boot 4.0.7, Spring Cloud BOM/SCF, Resilience4j | Quarkus **3.33 LTS** (BOM), quarkus-rest-jackson, quarkus-hibernate-orm (ou Panache), quarkus-flyway, quarkus-smallrye-jwt, quarkus-smallrye-health, quarkus-amazon-lambda, quarkus-messaging-kafka (local) / SQS SDK v2, smallrye-fault-tolerance, quarkus-jacoco |
| Consistency Conventions | Config `application-{local\|aws}.yml`; "Testes: JUnit 5 + Mockito" | `application.properties` com profiles `%local`/`%aws` (ou `quarkus-config-yaml`); `@QuarkusTest` + RestAssured na camada web; JUnit 5 + Mockito seguem nos unitários puros |
| Structural Seed / Starter | "Spring Boot modular monolith"; Spring Initializr | "Quarkus modular monolith"; `code.quarkus.io` / CLI `quarkus create` |

**O que NÃO muda:** contratos HTTP (AD-9, `/api/v1/`), contrato do evento de alerta (AD-5), janelas
de relatório (AD-6), modelo de erros `{code, message, traceId}`, migrações Flyway V1/V2 (SQL puro —
reaproveitadas como estão), ER diagram, S3 keys, chaves de secrets, envelope de VPC (AD-17).

### 2.4 UI/UX

N/A — projeto sem UI (Postman + e-mail).

### 2.5 Artefatos secundários / técnicos

| Artefato | Impacto |
| --- | --- |
| Código do módulo 01 | **Reimplementação em Quarkus no branch atual**, usando a spec openspec como fonte. Reaproveitar por cópia seletiva do branch Spring: `domain/` (Papel, Usuario, exceptions), `application/` (LoginUseCase, ListarAvaliacoesUseCase, DTOs, portas `CurrentUserProvider`/`TokenService`/`UsuarioRepository`/`AvaliacaoReadRepository`) e `db/migration/` (V1/V2) — tudo agnóstico de framework. Reescrever a casca: pom.xml (BOM Quarkus), segurança (SmallRye JWT + `@RolesAllowed`), adapters de persistência (Panache/EntityManager nas mesmas portas), web (JAX-RS + `ExceptionMapper` + `ContainerRequestFilter` para traceId), health (SmallRye Health), config (`%local`/`%aws`). O branch Spring **não é mergeado** — fica como referência/histórico. |
| Testes do módulo 01 | Unitários de use case (Mockito) copiados quase intactos; camada web reescrita com `@QuarkusTest` + RestAssured; helpers de JWT reescritos. Critério de saída: suíte verde + JaCoCo ≥ 90%. |
| CI/CD (GitHub Actions) | O workflow `ci(api)` também está só no branch Spring. Recriar no branch atual: `mvn verify` (JaCoCo via `quarkus-jacoco`), empacotamento fast-jar, `Dockerfile.jvm` → ECR. |
| Dockerfile / imagem ECS | Usar `Dockerfile.jvm` padrão Quarkus (build nativo GraalVM fica **fora do MVP** — custo/tempo; cold start tolerado pelo NFR do PRD). |
| CDK / infra | Ainda não existe. Nasce direto para Quarkus — sem retrabalho. Lambdas runtime `java17`; muda só o handler configurado vs plano anterior. |
| docker-compose local | Ainda não existe no branch; sem impacto (Postgres + Kafka, agnóstico). |
| Postman collection / roteiro do vídeo | Endpoints inalterados; atualizar slide de stack e a justificativa do modelo cloud (cena 2 — SM-2). |
| Working tree atual | Limpar resíduo `feedbacks/apps/api/target/` (build antigo, untracked) antes de começar. |

## 3. Abordagem recomendada (Recommended Approach)

### Opções avaliadas (checklist §4)

| Opção | Avaliação | Esforço | Risco |
| --- | --- | --- | --- |
| **1. Direct Adjustment** — re-baseline da arquitetura + módulo 01 reimplementado em Quarkus (com reaproveitamento seletivo) + módulos seguintes já em Quarkus | **Viável e recomendada** (se a troca for confirmada) | Médio-alto: ~2 dias para fundação + módulo 01 com testes e CI verdes | **Alto por causa do prazo**: consome ~25–30% dos **7 dias** restantes sem entregar FR novo |
| 2. Rollback | N/A no sentido clássico — nada foi mergeado; o equivalente é **desistir da troca**: mergear o branch Spring e seguir em Spring (custo zero de retrabalho) | Baixo | Baixo |
| 3. MVP Review | Não necessário — o MVP continua o mesmo em qualquer framework; nenhum FR sai do escopo por causa da troca | — | — |

### Recomendação

**Direct Adjustment**, com go/no-go explícito como primeira decisão: a troca **não é exigida pelo
enunciado** (o Tech Challenge pede cloud + serverless + segurança + deploy + monitoramento, não um
framework específico) e o prazo é 28/07 — **7 dias**. Vale a pena se o motivo for aprendizado +
narrativa técnica no vídeo (cold start menor e footprint reduzido em Lambda/container é um argumento
real e filmável, que fortalece SM-2). Se o motivo for fraco, a alternativa racional é mergear o
branch Spring e seguir.

Mitigadores que tornam a mudança viável no prazo:

1. **O código Spring nunca foi mergeado** — o custo é de reimplementação orientada por spec, não de
   migração de base integrada; o develop permanece limpo, sem commits "troca de framework".
2. Arquitetura hexagonal (AD-2) isola o retrabalho na casca: `domain/` e `application/` do branch
   Spring são cópias diretas (zero dependência de framework).
3. Migrações Flyway, contratos HTTP, contrato de evento e todo o plano AWS/CDK são reaproveitados
   como estão — infra ainda nem foi criada.
4. Só o módulo 01 foi implementado — o custo de troca **nunca será menor do que agora**.

## 4. Propostas de mudança detalhadas (o que refazer, **em ordem**)

> Ordem de execução obrigatória — cada passo desbloqueia o seguinte. Estimativas para dev solo.

### Passo 0 — Go/no-go (decisão, 0h)

Confirmar que o benefício (aprendizado + narrativa serverless no vídeo) compensa ~2 dias de
retrabalho a **7 dias** da entrega. **Sem essa confirmação, nada abaixo é executado** — e o plano B
é mergear `feature/openspec-01-autenticacao-e-papeis-implementacao` e seguir em Spring.

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

- **AD-8**: trocar "Spring Security resource-server/filter" por "Quarkus Security + SmallRye JWT";
  decisão explícita: **manter HS256** (`mp.jwt.verify.publickey.algorithm=HS256`; secret continua em
  Secrets Manager com chave `jwtSecret`) — não tocar AD-12 nem o seed de secrets.
- **AD-18**: Resilience4j → SmallRye Fault Tolerance (mesma regra, anotações MicroProfile).
- **AD-11**: Actuator → SmallRye Health (`/q/health` com checagem de DB); expor em path compatível
  com o ALB health check.
- **AD-1/AD-2**: ajustes de texto ("Spring Boot" → "Quarkus"; "wiring Spring" → "CDI/ArC").
- **Tabela Stack + Starter + Structural Seed + Consistency Conventions (linhas Testes e Config)**:
  nova stack Quarkus 3.33 LTS (ver §2.3); `application.properties` com profiles `%local`/`%aws`.
- Diagrama do Design Paradigm: "API Spring Boot / ECS" → "API Quarkus / ECS"; linhas das Lambdas
  "Spring Boot + SCF" → "Quarkus + quarkus-amazon-lambda".

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

- `briefs/brief-adjt-fase4-2026-07-19/addendum.md` — adicionar nota "stack superseded pela Sprint
  Change Proposal 2026-07-21" (preservar histórico da decisão original).
- PRD corpo: **nenhuma mudança** (nenhum FR/UJ cita framework).

### Passo 3 — Spec do módulo 01 (0,5–1h) — `openspec/modules/01-autenticacao-e-papeis/`

- `design.md`: trocar stack (linha 6), diagrama (`SpringCurrentUserProvider` → provider via
  `SecurityIdentity`), estrutura de arquivos, §7 (config Spring Security → SmallRye JWT +
  `@RolesAllowed`), pom de exemplo e a decisão de biblioteca JWT (§ decisões: JJWT vs Spring OAuth2
  → `smallrye-jwt-build`). Matriz de autorização, contratos e erros permanecem.
- `proposal.md`: linha 49 "(Spring Security)" → "(Quarkus Security)"; linha 92
  "`@PreAuthorize`/`authorizeHttpRequests`" → "`@RolesAllowed` server-side".
- `specs.md`: revisar apenas se citar mecanismo — comportamento observável (401/403, claims, seed)
  não muda.

### Passo 4 — Módulo 01 em Quarkus (1–1,5 dia) — novo código no branch atual

Ordem interna sugerida:

1. Scaffold: `quarkus create` com BOM 3.33 LTS; extensões `quarkus-rest-jackson`,
   `quarkus-hibernate-orm`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-smallrye-jwt` +
   `smallrye-jwt-build`, `quarkus-hibernate-validator`, `quarkus-smallrye-health`,
   `quarkus-jacoco`, `quarkus-junit5` + RestAssured; gate JaCoCo 90% no `pom.xml`.
2. Copiar do branch Spring (cópia direta, sem merge): `domain/`, `application/` (casos de uso,
   DTOs, portas) e `src/main/resources/db/migration/` (V1/V2).
3. Config: `application.properties` com profiles `%local`/`%aws` (equivalente ao
   `application-{local|aws}.yml` planejado).
4. Segurança: emissão de JWT com `smallrye-jwt-build` implementando a porta `TokenService`;
   validação via `quarkus-smallrye-jwt` (HS256 configurado); rotas protegidas com `@RolesAllowed`;
   provider de usuário corrente via `SecurityIdentity` implementando `CurrentUserProvider`.
5. Persistência: adapters Panache/`EntityManager` implementando as mesmas portas
   (`UsuarioRepository`, `AvaliacaoReadRepository`); entidades JPA reaproveitadas.
6. Web: controllers JAX-RS (`@Path`) com os mesmos contratos; `ExceptionMapper`s reproduzindo o
   corpo de erro `{code, message, traceId}`; `ContainerRequestFilter` para traceId.
7. Health: SmallRye Health com check de DB.
8. Testes: unitários de use case copiados; camada web com `@QuarkusTest` + RestAssured; helpers JWT
   novos. **Critério de saída: suíte verde + JaCoCo ≥ 90% + mesma superfície HTTP do design.**

### Passo 5 — CI/CD e empacotamento (0,5 dia)

- Recriar GitHub Actions no branch atual (workflow antigo está só no branch Spring): `mvn verify`
  com JaCoCo, empacotar fast-jar, `Dockerfile.jvm` → ECR.
- Build nativo GraalVM: **registrar como Deferred** no Spine (custo de build alto; cold start em
  Lambda tolerado pelo NFR do PRD — Deferred "SnapStart/Graal" já existe e permanece).

### Passo 6 — Módulos seguintes já em Quarkus (sem retrabalho)

- Módulos 02+ (catálogo, inscrição, avaliação, publish) seguem a nova fundação:
  `quarkus-messaging-kafka` no profile `%local`, SQS SDK v2 no `%aws`, porta
  `EvaluationEventPublisher` inalterada (AD-5/AD-7 preservados).
- Lambdas (`notification`, `report`): criar com `quarkus-amazon-lambda` conforme novo AD-13;
  OpenPDF continua na `lambda-report`.

### Passo 7 — Roteiro do vídeo (0,5h)

- Cenas 1–2: atualizar slide da stack; incorporar justificativa "Quarkus para startup rápido/baixo
  footprint em container e Lambda" — fortalece a exigência de explicar o modelo cloud (SM-2).

**Esforço total estimado: ~2 a 2,5 dias** de um total de **7** até a entrega (28/07). Sobram ~4,5–5
dias para módulos 02+, infra CDK, deploy e gravação — apertado, porém factível para o escopo MVP.

## 5. Handoff de implementação

**Classificação de escopo: MAJOR** — muda decisões adotadas de arquitetura (AD-13, AD-8, AD-18) e
re-baseline da stack, embora não altere escopo de produto.

| Papel | Responsabilidade |
| --- | --- |
| Architect (skill `bmad-architecture`, intent update) | Passo 1 — re-baseline do ARCHITECTURE-SPINE com os ADs revisados desta proposta |
| PM/Analyst (edição direta) | Passo 2 — addendums de PRD e brief |
| Developer (skill `bmad-quick-dev` ou `bmad-dev-story`) | Passos 3–5 — spec do módulo 01, reimplementação Quarkus, CI |
| Developer (fluxo normal de módulos) | Passo 6 — módulos seguintes na nova fundação |
| Thiago (conteúdo) | Passo 7 — slide/roteiro do vídeo |

**Critérios de sucesso da mudança:**

1. ARCHITECTURE-SPINE atualizado sem referência residual a Spring (exceto histórico/registro).
2. Módulo 01 em Quarkus no branch `feature/replanejamento-quarkus`: mesma superfície HTTP e mesmos
   códigos de erro do design, suíte verde, JaCoCo ≥ 90%.
3. Pipeline CI verde com build Quarkus.
4. Nenhum FR do PRD alterado; Postman collection do módulo 01 funciona sem mudança.
5. Branch Spring preservado como referência, **não mergeado** no develop.

## 6. Status do checklist (registro)

| Seção | Status | Nota |
| --- | --- | --- |
| 1. Trigger e contexto | [x] Done | Pivô estratégico de stack; evidências em §1, incluindo estado real dos branches |
| 2. Impacto em épicos | [x] Done (adaptado) | Sem épicos formais; análise sobre módulos openspec (§2.1). sprint-status.yaml inexistente → item 6.4 N/A |
| 3. Conflitos em artefatos | [x] Done | PRD ok; addendums, arquitetura, spec 01, código, CI mapeados (§2.2–2.5). UX N/A |
| 4. Caminho adiante | [x] Done | Direct Adjustment recomendado com go/no-go explícito; rollback = permanecer em Spring mergeando o branch existente (§3) |
| 5. Componentes da proposta | [x] Done | Este documento |
| 6. Revisão final e aprovação | [x] Done | **Aprovada por ThiagoFerreira em 2026-07-21**; item 6.4 (sprint-status.yaml) N/A — arquivo inexistente |
