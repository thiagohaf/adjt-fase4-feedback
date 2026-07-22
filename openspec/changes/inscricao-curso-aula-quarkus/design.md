# Design — Implementar Módulo 03: Inscrição Curso/Aula (Quarkus)

> O design técnico completo já existe em
> `openspec/modules/03-inscricao-curso-aula/design.md` (visão §1, endpoints §2, pacotes §3,
> domínio §4, Flyway V4 §5, DTOs §6, portas/use cases §7, exceções §8, testes §9, Postman §10).
> **Este documento não o duplica** — registra decisões de execução desta change e defaults
> fechados do módulo (§11).

## Context

- Módulos 01 e 02 mergeados em `develop`: auth JWT estável; catálogo com `curso`/`aula` (V3);
  stub `POST .../cursos/{id}/inscricoes` e stub `POST /avaliacoes` sem gate.
- Docs do módulo 03 em `openspec/modules/03-inscricao-curso-aula/` (esta change).
- Specs principais auth + catalog OK em `openspec/specs/` — **não alterar** requisitos
  `AUTH_*` / `catalog-*`.
- Branch: `feature/openspec-03-inscricao-curso-aula` a partir de `develop`.

## Goals / Non-Goals

**Goals:**

- Substituir stub de inscrição em Curso por hexágono + JPA + Flyway V4.
- Entregar inscrição em Aula com pré-condição (inscrição no Curso) e UNIQUEs (AD-15).
- Expor porta de verificação + gate FR-6 no stub de Avaliação.
- Checkboxes do proposal.md §6 do módulo verificáveis; specs cobertos por teste; JaCoCo ≥ 90%.

**Non-Goals:**

- Avaliação completa (FR-7+), Urgência, publish.
- Listagem/cancelamento de inscrição; Admin se inscrever.
- Alterar `AuthResource`, claims JWT, códigos `AUTH_*`, V1–V3 Flyway, contrato Curso/Aula.

## Decisions

### D1 — Dois agregados / duas tabelas (AD-15)

`inscricao_curso(estudante_id, curso_id)` e `inscricao_aula(estudante_id, aula_id)` com
**UNIQUE** em cada par. Criar inscrição em Aula **exige** inscrição no Curso da Aula.
Duplicata → **409** `INSCRICAO_DUPLICADA` (não idempotente). `curso_id` denormalizado em
`inscricao_aula`.

### D2 — Paths e roles

- Manter `POST /api/v1/cursos/{cursoId}/inscricoes` + `@RolesAllowed("ESTUDANTE")`.
- Novo `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` + `@RolesAllowed("ESTUDANTE")`.
- Body vazio; `estudanteId` = `CurrentUserProvider.getCurrentUserId()`.

### D3 — Flyway somente V4

`V4__create_inscricao.sql` referenciando `usuario(id)`, `curso(id)`, `aula(id)`. Não reescrever
V1–V3. Sem seed de inscrição.

### D4 — Códigos de erro

| Exceção | HTTP | code |
| --- | --- | --- |
| `CursoNotFoundException` | 404 | `CURSO_NOT_FOUND` |
| `AulaNotFoundException` | 404 | `AULA_NOT_FOUND` |
| `InscricaoDuplicadaException` | 409 | `INSCRICAO_DUPLICADA` |
| `InscricaoCursoObrigatoriaException` | 409 | `INSCRICAO_CURSO_OBRIGATORIA` |
| `InscricaoAulaObrigatoriaException` | 403 | `INSCRICAO_AULA_OBRIGATORIA` |
| Bean Validation | 400 | `VALIDATION_ERROR` |

`AUTH_*` intactos. Mapear UNIQUE violation → `INSCRICAO_DUPLICADA`.

### D5 — Gate FR-6 no stub (sem FR-7)

`AvaliacaoResource.criar` exige `aulaId` no body; consulta
`VerificarInscricaoAulaUseCase`; sem inscrição → 403 `INSCRICAO_AULA_OBRIGATORIA`; com
inscrição → mantém 201 stub (sem persistir Avaliação de domínio / Urgência).

### D6 — Pacotes conforme design do módulo §3/§7

`application/enrollment`, `domain/enrollment`, DTOs de response, ExceptionMappers novos.
Reutilizar `CursoRepository` / `AulaRepository` para existence e pertencimento.

### D7 — Testes e Postman

Unit nos use cases; `@QuarkusTest` para SPEC-5.x e SPEC-6.x; adaptar SPEC-2.9/auth que
dependiam do stub sem Curso. Postman: inscrição Curso real + inscrição Aula no UJ-1.

## Risks / Trade-offs

- [Auth tests 201 no stub sem Curso] → Arrange cria Curso (e inscrição Curso antes de Aula).
- [Race duplicata] → UNIQUE + mapper de constraint.
- [FR-6 vs FR-7] → Gate só; checklist do módulo deixa FR-7 explícito fora.
- [Aula de outro Curso] → Tratar como `AULA_NOT_FOUND` (não vazar existência cross-curso).

## Migration Plan

1. Branch a partir de `develop`; implementar V4 + domínio + resources + gate.
2. Atualizar testes e Postman na mesma change.
3. `mvn verify` verde com JaCoCo ≥ 90%.
4. PR para `develop`; após merge, archive OpenSpec da change.

Rollback: reverter PR; V4 é aditiva.

## Open Questions

Nenhuma — defaults do módulo §11 fechados: path Aula aninhado, body vazio, 409 duplicata /
pré-condição Curso, 403 gate Avaliação, sem listagem HTTP, sem FR-7.
