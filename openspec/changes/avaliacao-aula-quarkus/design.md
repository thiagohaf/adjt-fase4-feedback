# Design — Implementar Módulo 04: Avaliação de Aula (Quarkus)

> O design técnico completo vive em
> `openspec/modules/04-avaliacao-aula/design.md` (visão, endpoints, pacotes, domínio, Flyway V5,
> DTOs, portas, publish, testes, Postman). **Este documento não o duplica** — registra
> decisões de execução desta change e defaults fechados.

## Context

- Módulos 01–03 mergeados em `develop`: auth JWT; catálogo V3; inscrição V4 + gate FR-6 no
  stub `POST /avaliacoes` (201 provisório sem persistir nota/Urgência).
- Tabela `avaliacao` (V1) só tem `id`, `estudante_id`, `descricao` — insuficiente para FR-7+
  / AD-16 (`nota`, `urgencia`, `ocorrido_em`, `aula_id`).
- Docs do módulo 04 em `openspec/modules/04-avaliacao-aula/` (esta change).
- Specs `inscricao-gate-avaliacao` e `auth-role-authorization` OK — gate 403 e filtro de
  listagem por papel permanecem; SPEC-6.2 muda de stub → criação real.
- Branch: `feature/openspec-04-avaliacao-quarkus` a partir de `develop`.
- CI `ci(api)` já roda `mvn verify` + JaCoCo ≥ 90% em PR/`develop`.

## Goals / Non-Goals

**Goals:**

- Substituir stub de `POST /avaliacoes` por hexágono + JPA + Flyway V5.
- Derivar Urgência na mesma transação (FR-9); unicidade Estudante+Aula → 409 (AD-15).
- Publicar evento AD-5 após commit quando ALTA (porta + adapters); sem Lambda/SES.
- Enriquecer `GET /avaliacoes` (FR-8) mantendo filtro Admin vs próprias.
- Checkboxes do proposal do módulo §6 verificáveis; specs cobertos por teste; JaCoCo ≥ 90%.

**Non-Goals:**

- Lambda notificação / SES (FR-10); relatórios (FR-11+); ECR/CDK (FR-15 restante).
- Update/delete/upsert de Avaliação; listagem paginada/filtros avançados.
- Alterar `AuthResource`, claims JWT, códigos `AUTH_*`, V1–V4 Flyway, inscrição, catálogo.

## Decisions

### D1 — Evoluir `avaliacao` via Flyway V5 (não reescrever V1)

`V5__evolve_avaliacao.sql`: ADD `aula_id` (FK `aula`), `nota` SMALLINT CHECK 0–10,
`urgencia` VARCHAR CHECK IN (`ALTA`,`MEDIA`,`BAIXA`), `ocorrido_em` TIMESTAMPTZ;
UNIQUE `(estudante_id, aula_id)`; índice por `ocorrido_em` (AD-16). Sem seed. V1–V4 intactos.

Alternativa rejeitada: dropar/recriar `avaliacao` — quebra histórico Flyway e testes que já
leem a tabela.

### D2 — Agregado `Avaliacao` + factory de Urgência no domain

`Avaliacao.criar(estudanteId, aulaId, cursoId, descricao, nota)` valida nota 0–10, deriva
`Urgencia` (enum), define `ocorridoEm = Instant.now()`. Sem setters públicos mutáveis após
criação no MVP.

### D3 — Fluxo do use case (AD-4 / AD-15)

`CriarAvaliacaoUseCase`: (1) `aulaId`/`descricao`/`nota` do body; (2) gate
`VerificarInscricaoAula`; (3) resolver Aula → `cursoId` (404 `AULA_NOT_FOUND`); (4) UNIQUE
check / save; (5) **após commit** (observer/`@TransactionalEvent` ou chamada pós-TX), se
ALTA → `EvaluationEventPublisher.publish(payload AD-5)`.

Publish **fora** da TX de escrita; falha → log + Fault Tolerance; Avaliação permanece.

### D4 — Porta `EvaluationEventPublisher` (AD-5 / AD-7)

Mesmo JSON: `avaliacaoId`, `descricao`, `urgencia`, `ocorridoEm` (ISO-8601 offset), `aulaId`,
`cursoId`. Adapters: Kafka `%local`; SQS `%aws` (ou stub no-op se fila ainda não provisionada —
documentar); teste com fake in-memory.

### D5 — Códigos de erro

| Exceção | HTTP | code |
| --- | --- | --- |
| Bean Validation (nota fora, campos) | 400 | `VALIDATION_ERROR` |
| `AulaNotFoundException` | 404 | `AULA_NOT_FOUND` |
| `InscricaoAulaObrigatoriaException` | 403 | `INSCRICAO_AULA_OBRIGATORIA` |
| `AvaliacaoDuplicadaException` | 409 | `AVALIACAO_DUPLICADA` |
| Auth (papel) | 403 | `AUTH_FORBIDDEN` |

`AUTH_*` e códigos de inscrição/catálogo intactos. UNIQUE violation → `AVALIACAO_DUPLICADA`.

### D6 — Listagem enriquecida

Estender entidade/read model: response com `id`, `estudanteId`, `aulaId`, `cursoId`,
`descricao`, `nota`, `urgencia`, `ocorridoEm`. Reusar `ListarAvaliacoesUseCase` + filtro por
papel; atualizar `AvaliacaoReadRepository` / JPA.

### D7 — Testes e Postman

Unit: Urgência fronteiras 4/5/7/8; use case sucesso/duplicata/sem inscrição/nota inválida.
`@QuarkusTest`: SPEC-7/8/9 + regressão gate SPEC-6.1; adaptar auth SPEC-2.7/2.10/2.11 ao
payload real. Postman: body com `nota`; assert Urgência; UJ-1 até 201 persistido.

## Risks / Trade-offs

- [V1 `avaliacao` sem colunas novas] → V5 ADD COLUMN; ambiente limpo em testes via Flyway.
- [SPEC-6.2 / testes do stub 201] → Atualizar asserts para payload real + persistência.
- [Publish sem SQS em CI] → Fake/no-op no profile test; Kafka só Compose local.
- [Race duplicata] → UNIQUE + mapper constraint → 409.
- [FR-10 confundido com publish] → Porta só enfileira; SES fica no módulo seguinte.

## Migration Plan

1. Branch a partir de `develop`; implementar V5 + domínio + resource + publish + listagem.
2. Atualizar testes (gate, auth listagem) e Postman na mesma change.
3. `mvn verify` verde com JaCoCo ≥ 90%.
4. PR para `develop`; após merge, archive OpenSpec da change.

Rollback: reverter PR; V5 é aditiva (colunas novas + UNIQUE).

## Open Questions

Nenhuma — defaults do módulo fechados: `aulaId` + `descricao` + `nota` no body; Urgência
enum `ALTA`/`MEDIA`/`BAIXA`; 409 duplicata; publish pós-commit só ALTA; FR-10 fora.
