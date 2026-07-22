# Spec — inscricao-aula

## Purpose

Inscrição de Estudante em Aula na API Quarkus (`feedbacks/apps/api/`):
`POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`, pré-requisito de inscrição
no Curso, códigos `INSCRICAO_CURSO_OBRIGATORIA` / `INSCRICAO_DUPLICADA` /
`AULA_NOT_FOUND` / `CURSO_NOT_FOUND` / `AUTH_*`. Derivado da change
`inscricao-curso-aula-quarkus` (módulo 03, FR-5 Aula).

## Requirements

### Requirement: Estudante se inscreve em Aula após inscrição no Curso

O sistema SHALL expor `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` com
`@RolesAllowed("ESTUDANTE")` que, dados Curso existente, Aula pertencente a esse Curso e
inscrição prévia do Estudante no Curso, persiste `InscricaoAula` e responde **201 Created**
com `id`, `cursoId`, `aulaId` e `estudanteId` (sub do JWT). Body vazio; identidade só via JWT.

#### Scenario: SPEC-5.5 — Inscrever em Aula com sucesso

- **WHEN** o Estudante já inscrito no Curso envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` para Aula desse Curso
- **THEN** a API responde 201 Created
- **AND** o corpo contém `id`, `cursoId`, `aulaId` e `estudanteId` igual ao `sub` do JWT

### Requirement: Inscrição em Aula sem inscrição no Curso retorna 409 INSCRICAO_CURSO_OBRIGATORIA

O sistema SHALL rejeitar inscrição em Aula quando o Estudante não estiver inscrito no Curso
do path, respondendo **409 Conflict** com `code` igual a `"INSCRICAO_CURSO_OBRIGATORIA"`,
sem persistir `InscricaoAula`.

#### Scenario: SPEC-5.6 — Inscrever em Aula sem inscrição no Curso

- **WHEN** o Estudante sem inscrição no Curso envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`
- **THEN** a API responde 409 Conflict com `code` igual a `"INSCRICAO_CURSO_OBRIGATORIA"`
- **AND** nenhuma inscrição em Aula é criada

### Requirement: Duplicata de inscrição em Aula retorna 409 INSCRICAO_DUPLICADA

O sistema SHALL rejeitar segunda inscrição do mesmo Estudante na mesma Aula com
**409 Conflict** e `code` igual a `"INSCRICAO_DUPLICADA"`. Unicidade MUST ter UNIQUE
`(estudante_id, aula_id)` no banco.

#### Scenario: SPEC-5.7 — Duplicata de inscrição em Aula

- **WHEN** o Estudante já inscrito na Aula envia novamente o POST de inscrição em Aula
- **THEN** a API responde 409 Conflict com `code` igual a `"INSCRICAO_DUPLICADA"`

### Requirement: Aula inexistente ou de outro Curso retorna 404 AULA_NOT_FOUND

O sistema SHALL responder **404 Not Found** com `code` igual a `"AULA_NOT_FOUND"` quando o
`aulaId` não existir ou a Aula não pertencer ao `cursoId` do path.

#### Scenario: SPEC-5.8 — Inscrever em Aula inexistente ou de outro Curso

- **WHEN** o Estudante envia inscrição em Aula com `aulaId` inexistente ou de outro Curso
- **THEN** a API responde 404 Not Found com `code` igual a `"AULA_NOT_FOUND"`

### Requirement: Curso inexistente na inscrição em Aula retorna 404 CURSO_NOT_FOUND

O sistema SHALL responder **404 Not Found** com `code` igual a `"CURSO_NOT_FOUND"` quando o
`cursoId` do path não existir.

#### Scenario: SPEC-5.9 — Inscrever em Aula com Curso inexistente

- **WHEN** o Estudante envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` com Curso inexistente
- **THEN** a API responde 404 Not Found com `code` igual a `"CURSO_NOT_FOUND"`

### Requirement: Administrador não se inscreve em Aula

O sistema SHALL negar a rota de inscrição em Aula para JWT com `role=ADMINISTRADOR`,
respondendo **403 Forbidden** com `code` igual a `"AUTH_FORBIDDEN"`.

#### Scenario: SPEC-5.10 — Administrador não se inscreve em Aula

- **WHEN** o Administrador envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

### Requirement: Inscrição em Aula exige autenticação

O sistema SHALL responder **401 Unauthorized** com código `AUTH_*` quando a requisição de
inscrição em Aula não trouxer JWT válido.

#### Scenario: SPEC-5.11 — Inscrição exige autenticação (Aula)

- **WHEN** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes` sem JWT
- **THEN** a API responde 401 Unauthorized com `code` do mapa `AUTH_*`
