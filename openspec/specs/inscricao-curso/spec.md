# Spec — inscricao-curso

## Purpose

Inscrição de Estudante em Curso na API Quarkus (`feedbacks/apps/api/`):
`POST /api/v1/cursos/{cursoId}/inscricoes`, persistência Flyway V4, códigos
`CURSO_NOT_FOUND` / `INSCRICAO_DUPLICADA` / `AUTH_*`, e papel `ESTUDANTE`.
Derivado da change `inscricao-curso-aula-quarkus` (módulo 03, FR-5 Curso).

## Requirements

### Requirement: Estudante se inscreve em Curso existente

O sistema SHALL expor `POST /api/v1/cursos/{cursoId}/inscricoes` com
`@RolesAllowed("ESTUDANTE")` que, dado Curso existente e JWT válido, persiste
`InscricaoCurso` vinculando o `sub` do token ao `cursoId` e responde **201 Created** com
corpo contendo `id` (UUID), `cursoId` e `estudanteId` (igual ao `sub`). O request NÃO
aceita `estudanteId` no body — a identidade vem exclusivamente do JWT.

#### Scenario: SPEC-5.1 — Inscrever em Curso com sucesso

- **WHEN** o Estudante com JWT válido envia `POST /api/v1/cursos/{cursoId}/inscricoes` para um Curso existente
- **THEN** a API responde 201 Created
- **AND** o corpo contém `id` (UUID), `cursoId` igual ao path e `estudanteId` igual ao `sub` do JWT

### Requirement: Inscrever em Curso inexistente retorna 404 CURSO_NOT_FOUND

O sistema SHALL responder **404 Not Found** com `code` igual a `"CURSO_NOT_FOUND"` quando o
`cursoId` do path não existir, sem persistir inscrição.

#### Scenario: SPEC-5.2 — Inscrever em Curso inexistente

- **WHEN** o Estudante envia `POST /api/v1/cursos/{cursoId}/inscricoes` com `cursoId` inexistente
- **THEN** a API responde 404 Not Found com `code` igual a `"CURSO_NOT_FOUND"`
- **AND** nenhuma inscrição é criada

### Requirement: Duplicata de inscrição em Curso retorna 409 INSCRICAO_DUPLICADA

O sistema SHALL rejeitar segunda inscrição do mesmo Estudante no mesmo Curso com
**409 Conflict** e `code` igual a `"INSCRICAO_DUPLICADA"` no envelope
`{ code, message, traceId }`. O comportamento NÃO é idempotente (não retorna 200/201 na
duplicata). A unicidade MUST ser garantida também por UNIQUE no banco.

#### Scenario: SPEC-5.3 — Duplicata de inscrição em Curso

- **WHEN** o Estudante já inscrito no Curso envia novamente `POST /api/v1/cursos/{cursoId}/inscricoes`
- **THEN** a API responde 409 Conflict com `code` igual a `"INSCRICAO_DUPLICADA"`

### Requirement: Administrador não se inscreve em Curso

O sistema SHALL negar a rota de inscrição em Curso para JWT com `role=ADMINISTRADOR`,
respondendo **403 Forbidden** com `code` igual a `"AUTH_FORBIDDEN"`.

#### Scenario: SPEC-5.4 — Administrador não se inscreve em Curso

- **WHEN** o Administrador com JWT válido envia `POST /api/v1/cursos/{cursoId}/inscricoes`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

### Requirement: Inscrição em Curso exige autenticação

O sistema SHALL responder **401 Unauthorized** com código `AUTH_*` do módulo 01 quando a
requisição de inscrição em Curso não trouxer JWT válido.

#### Scenario: SPEC-5.11 — Inscrição exige autenticação (Curso)

- **WHEN** o cliente envia `POST /api/v1/cursos/{cursoId}/inscricoes` sem JWT
- **THEN** a API responde 401 Unauthorized com `code` do mapa `AUTH_*`
