# Spec — auth-role-authorization

## Purpose

Autorização por papéis (`ESTUDANTE` | `ADMINISTRADOR`) via `@RolesAllowed`, envelope
`AUTH_FORBIDDEN` e filtro fino de listagem de Avaliações com `CurrentUserProvider`. Recursos
de Curso/Aula/Inscrição/Avaliação (escrita) existem como stubs mínimos na API Quarkus; a regra
fina SPEC-2.10/2.11 usa read model mínimo de Avaliação. Implementado na change
`autenticacao-e-papeis-quarkus`.

## Requirements

### Requirement: Criação de Curso e Aula exige papel ADMINISTRADOR

O sistema SHALL proteger `POST /api/v1/cursos` e `POST /api/v1/cursos/{cursoId}/aulas` com
`@RolesAllowed("ADMINISTRADOR")`, respondendo 403 Forbidden com envelope
`{ code: "AUTH_FORBIDDEN", message, traceId }` para token válido de papel `ESTUDANTE`.

#### Scenario: SPEC-2.1 — Estudante não cria Curso

- **WHEN** o cliente envia `POST /api/v1/cursos` com JWT válido de `role=ESTUDANTE`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`
- **AND** nenhum Curso é criado

#### Scenario: SPEC-2.2 — Estudante não cria Aula

- **WHEN** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas` com JWT válido de `role=ESTUDANTE`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

#### Scenario: SPEC-2.3 — Administrador cria Curso e Aula

- **WHEN** o cliente envia `POST /api/v1/cursos` ou `POST /api/v1/cursos/{cursoId}/aulas` com JWT válido de `role=ADMINISTRADOR`
- **THEN** a API não retorna 403 por autorização
- **AND** processa a criação (201 ou erro de validação de negócio, nunca 403)

### Requirement: Leitura de catálogo é permitida a ambos os papéis

O sistema SHALL permitir `GET` no catálogo (ex.: `GET /api/v1/cursos`,
`GET /api/v1/cursos/{id}/aulas`) para qualquer usuário autenticado (`ESTUDANTE` ou
`ADMINISTRADOR`), sem exigir inscrição.

#### Scenario: SPEC-2.4 — Administrador consulta catálogo sem inscrição

- **WHEN** o cliente envia `GET /api/v1/cursos` ou `GET /api/v1/cursos/{id}/aulas` com JWT válido de Administrador não inscrito em nada
- **THEN** a API responde 200 OK com a listagem/consulta

#### Scenario: SPEC-2.5 — Estudante consulta catálogo

- **WHEN** o cliente envia `GET /api/v1/cursos` (ou consulta por id) com JWT válido de Estudante
- **THEN** a API responde 200 OK (ou 404 se id inexistente — nunca 403)

### Requirement: Criação de Avaliação e Inscrição exigem papel ESTUDANTE

O sistema SHALL proteger `POST /avaliacao` e as rotas de inscrição
(ex.: `POST /api/v1/cursos/{id}/inscricoes`) com `@RolesAllowed("ESTUDANTE")`, respondendo 403
`AUTH_FORBIDDEN` para token válido de papel `ADMINISTRADOR`.

#### Scenario: SPEC-2.6 — Administrador não cria Avaliação

- **WHEN** o cliente envia `POST /avaliacao` com JWT válido de `role=ADMINISTRADOR`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

#### Scenario: SPEC-2.7 — Estudante cria Avaliação (autorização OK)

- **WHEN** o cliente envia `POST /avaliacao` com JWT válido de `role=ESTUDANTE`
- **THEN** a API não retorna 403 por autorização

#### Scenario: SPEC-2.8 — Administrador não se inscreve

- **WHEN** o cliente envia `POST /api/v1/cursos/{id}/inscricoes` (ou rota equivalente de inscrição) com JWT válido de Administrador
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

#### Scenario: SPEC-2.9 — Estudante realiza inscrição

- **WHEN** o cliente envia request de inscrição em Curso/Aula com JWT válido de Estudante
- **THEN** a API não retorna 403 por autorização

### Requirement: Listagem de Avaliações filtra por papel via CurrentUserProvider

O sistema SHALL expor `GET /avaliacao` com
`@RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})` e aplicar a regra fina no caso de uso (não só na
anotação, conforme AD-8): Administrador recebe Avaliações de todos os Estudantes; Estudante
recebe apenas as próprias, identificadas pelo `sub` do token via porta `CurrentUserProvider`
(adapter sobre `SecurityIdentity`/`JsonWebToken`).

#### Scenario: SPEC-2.10 — Admin vê todas as Avaliações

- **WHEN** existem Avaliações de múltiplos Estudantes e o cliente envia `GET /avaliacao` com JWT válido de Administrador
- **THEN** a API retorna Avaliações de todos os Estudantes

#### Scenario: SPEC-2.11 — Estudante vê só as próprias

- **WHEN** existem Avaliações do Estudante A e do Estudante B e o cliente envia `GET /avaliacao` com JWT válido do Estudante A
- **THEN** a API retorna apenas Avaliações criadas pelo Estudante A
- **AND** nenhuma Avaliação do Estudante B é incluída

### Requirement: Negação de papel produz envelope AUTH_FORBIDDEN

O sistema SHALL mapear a negação de `@RolesAllowed` (`ForbiddenException` do Quarkus Security) e
a `ForbiddenAccessException` de domínio para 403 com envelope
`{ code: "AUTH_FORBIDDEN", message, traceId }`.

#### Scenario: 403 com envelope padronizado

- **WHEN** qualquer rota nega acesso por papel insuficiente
- **THEN** a resposta é 403 com corpo `{ code: "AUTH_FORBIDDEN", message, traceId }` (traceId não vazio)
