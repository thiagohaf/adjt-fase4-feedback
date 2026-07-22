# Spec — catalog-curso

## Purpose

CRUD de consulta/criação de Curso na API Quarkus (`feedbacks/apps/api/`): persistência
PostgreSQL via Flyway V3, campo público `nome`, códigos `CURSO_NOT_FOUND` /
`VALIDATION_ERROR`, e autorização com papéis do módulo 01. Derivado da change
`catalogo-curso-aula-quarkus` (módulo 02, FR-3/FR-4).

## Requirements

### Requirement: Administrador cria Curso com nome obrigatório e descricao opcional

O sistema SHALL expor `POST /api/v1/cursos` protegido com `@RolesAllowed("ADMINISTRADOR")`
que, dado payload válido (`nome` obrigatório não-branco após trim; `descricao` opcional),
persiste um Curso e responde **201 Created** com corpo contendo `id` (UUID), `nome` e
`descricao` (nullable quando omitida).

#### Scenario: SPEC-3.1 — Criar Curso com sucesso

- **WHEN** o Administrador com JWT válido envia `POST /api/v1/cursos` com `{"nome": "Arquitetura Cloud", "descricao": "Curso introdutório"}`
- **THEN** a API responde 201 Created
- **AND** o corpo contém `id` (UUID não nulo), `nome` igual a `"Arquitetura Cloud"` e `descricao` igual a `"Curso introdutório"`
- **AND** o Curso fica disponível em `GET /api/v1/cursos` e `GET /api/v1/cursos/{id}`

#### Scenario: SPEC-3.2 — Criar Curso só com nome

- **WHEN** o Administrador envia `POST /api/v1/cursos` com `{"nome": "Somente Nome"}`
- **THEN** a API responde 201 Created
- **AND** `nome` é `"Somente Nome"`
- **AND** `descricao` é `null`

### Requirement: Criar Curso sem nome válido retorna 400 VALIDATION_ERROR

O sistema SHALL validar `nome` (`@NotBlank` / não vazio após trim) e responder
**400 Bad Request** com envelope `{ code: "VALIDATION_ERROR", message, traceId }` quando a
validação falhar, sem persistir Curso.

#### Scenario: SPEC-3.3 — Criar Curso sem nome

- **WHEN** o Administrador envia `POST /api/v1/cursos` sem `nome`, com `nome` em branco ou só espaços
- **THEN** a API responde 400 Bad Request com `code` igual a `"VALIDATION_ERROR"`
- **AND** nenhum Curso é criado

### Requirement: Estudante não cria Curso

O sistema SHALL negar `POST /api/v1/cursos` para JWT com `role=ESTUDANTE`, respondendo
**403 Forbidden** com `code` igual a `"AUTH_FORBIDDEN"` (código do módulo 01, inalterado).

#### Scenario: SPEC-3.4 — Estudante não cria Curso

- **WHEN** o Estudante com JWT válido envia `POST /api/v1/cursos`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

### Requirement: Listar e consultar Curso para Estudante e Administrador

O sistema SHALL expor `GET /api/v1/cursos` e `GET /api/v1/cursos/{id}` com
`@RolesAllowed({"ESTUDANTE","ADMINISTRADOR"})`. A listagem retorna array JSON (ordem estável
por `criado_em ASC`); cada item contém pelo menos `id` e `nome`. A consulta por id retorna
`id`, `nome` e `descricao` (nullable).

#### Scenario: SPEC-4.1 — Listar Cursos

- **WHEN** o cliente autenticado (Estudante ou Admin) envia `GET /api/v1/cursos`
- **THEN** a API responde 200 OK com corpo array JSON
- **AND** cada item contém pelo menos `id` e `nome`

#### Scenario: SPEC-4.2 — Consultar Curso por id

- **WHEN** o cliente autenticado envia `GET /api/v1/cursos/{id}` de um Curso existente
- **THEN** a API responde 200 OK com `id`, `nome` e `descricao` (nullable)

### Requirement: Consultar Curso inexistente retorna 404 CURSO_NOT_FOUND

O sistema SHALL responder **404 Not Found** com `code` igual a `"CURSO_NOT_FOUND"` no
envelope padrão quando o id informado não existir.

#### Scenario: SPEC-4.3 — Consultar Curso inexistente

- **WHEN** o cliente autenticado envia `GET /api/v1/cursos/{id}` com id inexistente
- **THEN** a API responde 404 Not Found com `code` igual a `"CURSO_NOT_FOUND"`

### Requirement: Rotas de Curso exigem autenticação

O sistema SHALL responder **401 Unauthorized** com `code` igual a `"AUTH_MISSING_TOKEN"`
quando `GET` ou `POST` de cursos for chamado sem header `Authorization` (comportamento do
módulo 01 — regressão mínima).

#### Scenario: SPEC-4.8 — Catálogo Curso sem token

- **WHEN** o cliente envia `GET /api/v1/cursos` ou `POST /api/v1/cursos` sem `Authorization`
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_MISSING_TOKEN"`

### Requirement: Curso persiste após reinício

O sistema SHALL persistir Curso em PostgreSQL via Flyway V3 de forma que, após reinício da
aplicação com o mesmo banco, o recurso continue consultável pelo mesmo `id`.

#### Scenario: SPEC-NFR-C1 — Persistência real de Curso

- **WHEN** um Curso é criado via API e a aplicação reinicia com o mesmo banco
- **THEN** o Curso continua consultável pelo mesmo `id`
