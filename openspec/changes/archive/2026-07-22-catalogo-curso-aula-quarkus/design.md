# Design — Implementar Módulo 02: Catálogo Curso/Aula (Quarkus)

> O design técnico completo já existe em
> `openspec/modules/02-catalogo-curso-aula/design.md` (visão §1, endpoints §3, pacotes §4,
> domínio §5, Flyway V3 §6, DTOs §7, portas/use cases §8, exceções §9, testes §10, Postman §11).
> **Este documento não o duplica** — registra decisões de execução desta change e defaults
> fechados do módulo (§12).

## Context

- Módulo 01 mergeado em `develop`: auth JWT estável; stubs em `CursoResource` / `AulaResource`
  com Aula usando `titulo` (provisório).
- Docs do módulo 02 já em `openspec/modules/02-catalogo-curso-aula/` (PR #10).
- Specs principais auth OK em `openspec/specs/` — **não alterar** requisitos `AUTH_*`.
- Branch sugerida: `feature/openspec-02-catalogo-curso-aula-quarkus` a partir de `develop`.

## Goals / Non-Goals

**Goals:**

- Substituir stubs de catálogo por hexágono + JPA + Flyway V3 conforme design do módulo.
- Contrato HTTP da Aula com campo **`nome`**; novo `GET /api/v1/cursos/{id}`.
- Checkboxes do proposal.md §6 do módulo verificáveis; specs cobertos por teste; JaCoCo ≥ 90%.

**Non-Goals:**

- Inscrição (FR-5/6), Avaliação, alertas, relatórios.
- Alterar `AuthResource`, claims JWT, códigos `AUTH_*`, V1/V2 Flyway.
- Update/Delete/seed de catálogo; unicidade de `nome`.

## Decisions

### D1 — Campo Aula `nome` (não `titulo`)

Stub e Postman usam `titulo`; PRD/Spine exigem `nome`. Contrato público da Aula usa **`nome`**
em request/response. Quebra deliberada do stub provisório — não do módulo 01.

*Alternativa descartada:* manter `titulo` e mapear internamente — divergiria do PRD e do
roteiro UJ-2.

### D2 — Paths e `@RolesAllowed` dos stubs + `GET /cursos/{id}`

Preservar paths/`@RolesAllowed` existentes. Incluir **`GET /api/v1/cursos/{id}`** (stub não
tinha; FR-3/FR-4). Stub `POST .../inscricoes` permanece sem lógica de domínio.

### D3 — Flyway somente V3

`V3__create_catalogo.sql` com tabelas `curso` e `aula` (FK `aula.curso_id → curso.id`, índice
`idx_aula_curso_id`). Não reescrever V1/V2. Sem seed de catálogo no MVP (criar via API/Postman).

### D4 — Exceções de domínio por agregado

Estender ExceptionMappers do módulo 01:

| Exceção | HTTP | code |
| --- | --- | --- |
| Bean Validation | 400 | `VALIDATION_ERROR` |
| `CursoNotFoundException` | 404 | `CURSO_NOT_FOUND` |
| `AulaNotFoundException` | 404 | `AULA_NOT_FOUND` |

Não introduzir `NOT_FOUND` genérico. `AUTH_*` intactos.

### D5 — Pacotes e use cases conforme design do módulo §4/§8

Pacotes `api/web/catalog`, `application/catalog` (+ `port`), `domain/catalog`,
`domain/exception`, `infrastructure/persistence`. Seis use cases: Criar/Listar/Consultar Curso
e Criar/ListarAulasDoCurso/Consultar Aula. Ordem de listagem: `criado_em ASC`.
`descricao` omitida → `null` no JSON (Jackson default).

### D6 — Testes e Postman

Unit (Mockito) nos use cases críticos; `@QuarkusTest` + RestAssured cobrindo SPECs 3.x/4.x
e 403/401 de regressão auth. Atualizar Postman: body Aula com `nome`; request
`GET /cursos/{{cursoId}}`; capturar `cursoId`/`aulaId` nos 201.

## Risks / Trade-offs

- [Quebrar asserts de auth que dependem de body stub/`titulo`] → Manter paths/roles; adaptar
  só assertions de corpo nos testes de catálogo/auth que inspecionam payload.
- [Esquecer `GET /cursos/{id}`] → Spec + task explícitas; checklist §6 do módulo.
- [Tocar V1/V2 por engano] → Só criar V3; review do diff de migration.

## Migration Plan

1. Branch a partir de `develop`; implementar V3 + domínio + resources.
2. Atualizar testes e Postman na mesma change.
3. `mvn verify` verde com JaCoCo ≥ 90%.
4. PR para `develop`; após merge, archive OpenSpec da change.

Rollback: reverter PR; V3 é aditiva (drop tables só se necessário em ambiente local).

## Open Questions

Nenhuma — defaults do módulo §12 fechados: `nome`, `GET /cursos/{id}`, sem seed,
`criado_em ASC`, `VARCHAR(1000)` em `descricao`, stub inscrição mantido.
