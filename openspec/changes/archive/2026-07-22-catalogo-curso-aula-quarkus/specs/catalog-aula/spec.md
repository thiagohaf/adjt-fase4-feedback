# Spec — catalog-aula

> Deriva de `openspec/modules/02-catalogo-curso-aula/specs.md` (FR-3/FR-4 para Aula:
> SPEC-3.5–3.9, SPEC-4.4–4.7, SPEC-4.8, SPEC-NFR-C*). Campo público é **`nome`** (não `titulo`).
> Cada cenário DEVE virar teste.

## ADDED Requirements

### Requirement: Administrador cria Aula com nome (não titulo) vinculada a Curso existente

O sistema SHALL expor `POST /api/v1/cursos/{cursoId}/aulas` protegido com
`@RolesAllowed("ADMINISTRADOR")` que, dado Curso existente e payload válido (`nome`
obrigatório; `descricao` opcional), persiste uma Aula e responde **201 Created** com
`id` (UUID), `cursoId` (igual ao path), `nome` e `descricao` (nullable). O JSON SHALL
usar o campo `nome` e NÃO o campo `titulo`.

#### Scenario: SPEC-3.5 — Criar Aula com sucesso

- **WHEN** existe um Curso com id `cursoId` e o Administrador envia `POST /api/v1/cursos/{cursoId}/aulas` com `{"nome": "Aula 1 — Containers", "descricao": "Docker e Kubernetes"}`
- **THEN** a API responde 201 Created
- **AND** o corpo contém `id` (UUID), `cursoId` igual ao path, `nome` e `descricao`
- **AND** o JSON não contém o campo `titulo`
- **AND** a Aula fica disponível em `GET /api/v1/cursos/{cursoId}/aulas` e `GET /api/v1/aulas/{id}`

#### Scenario: SPEC-3.6 — Criar Aula só com nome

- **WHEN** existe um Curso e o Administrador envia `POST /api/v1/cursos/{cursoId}/aulas` com `{"nome": "Aula mínima"}`
- **THEN** a API responde 201 Created
- **AND** `descricao` é `null`

### Requirement: Criar Aula sem nome válido retorna 400 VALIDATION_ERROR

O sistema SHALL validar `nome` e responder **400 Bad Request** com
`code` igual a `"VALIDATION_ERROR"` quando a validação falhar, sem persistir Aula.

#### Scenario: SPEC-3.7 — Criar Aula sem nome

- **WHEN** existe um Curso e o Administrador envia criação de Aula sem `nome` válido
- **THEN** a API responde 400 Bad Request com `code` igual a `"VALIDATION_ERROR"`

### Requirement: Criar Aula com Curso inexistente retorna 404 CURSO_NOT_FOUND

O sistema SHALL verificar existência do Curso do path antes de persistir Aula e, se ausente,
responder **404 Not Found** com `code` igual a `"CURSO_NOT_FOUND"`, sem criar Aula.

#### Scenario: SPEC-3.8 — Criar Aula com Curso inexistente

- **WHEN** não existe Curso com o `cursoId` informado e o Administrador envia `POST /api/v1/cursos/{cursoId}/aulas` com `nome` válido
- **THEN** a API responde 404 Not Found com `code` igual a `"CURSO_NOT_FOUND"`
- **AND** nenhuma Aula é criada

### Requirement: Estudante não cria Aula

O sistema SHALL negar `POST /api/v1/cursos/{cursoId}/aulas` para JWT com `role=ESTUDANTE`,
respondendo **403 Forbidden** com `code` igual a `"AUTH_FORBIDDEN"`.

#### Scenario: SPEC-3.9 — Estudante não cria Aula

- **WHEN** o Estudante com JWT válido envia `POST /api/v1/cursos/{cursoId}/aulas`
- **THEN** a API responde 403 Forbidden com `code` igual a `"AUTH_FORBIDDEN"`

### Requirement: Listar Aulas do Curso exige Curso existente

O sistema SHALL expor `GET /api/v1/cursos/{cursoId}/aulas` com
`@RolesAllowed({"ESTUDANTE","ADMINISTRADOR"})`. Se o Curso existir, responde **200 OK** com
array (cada item: `id`, `cursoId`, `nome`); se o Curso não existir, **404**
`CURSO_NOT_FOUND`.

#### Scenario: SPEC-4.4 — Listar Aulas de um Curso

- **WHEN** existe um Curso com zero ou mais Aulas e o cliente autenticado envia `GET /api/v1/cursos/{cursoId}/aulas`
- **THEN** a API responde 200 OK com array; cada item contém `id`, `cursoId`, `nome`

#### Scenario: SPEC-4.5 — Listar Aulas de Curso inexistente

- **WHEN** não existe Curso com o `cursoId` e o cliente autenticado envia `GET /api/v1/cursos/{cursoId}/aulas`
- **THEN** a API responde 404 Not Found com `code` igual a `"CURSO_NOT_FOUND"`

### Requirement: Consultar Aula por id

O sistema SHALL expor `GET /api/v1/aulas/{id}` com
`@RolesAllowed({"ESTUDANTE","ADMINISTRADOR"})` retornando `id`, `cursoId`, `nome` e
`descricao` (nullable), sem campo `titulo`. Id inexistente → **404** `AULA_NOT_FOUND`.

#### Scenario: SPEC-4.6 — Consultar Aula por id

- **WHEN** existe uma Aula com id conhecido e o cliente autenticado envia `GET /api/v1/aulas/{id}`
- **THEN** a API responde 200 OK com `id`, `cursoId`, `nome` e `descricao` (nullable)
- **AND** o JSON não contém o campo `titulo`

#### Scenario: SPEC-4.7 — Consultar Aula inexistente

- **WHEN** o cliente autenticado envia `GET /api/v1/aulas/{id}` com id inexistente
- **THEN** a API responde 404 Not Found com `code` igual a `"AULA_NOT_FOUND"`

### Requirement: Rotas de Aula exigem autenticação

O sistema SHALL responder **401 Unauthorized** com `code` igual a `"AUTH_MISSING_TOKEN"`
quando rotas de Aula forem chamadas sem `Authorization` (regressão do módulo 01).

#### Scenario: SPEC-4.8 — Catálogo Aula sem token

- **WHEN** o cliente envia `GET /api/v1/aulas/{id}` ou `POST /api/v1/cursos/{cursoId}/aulas` sem `Authorization`
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_MISSING_TOKEN"`

### Requirement: Aula persiste após reinício

O sistema SHALL persistir Aula em PostgreSQL via Flyway V3 de forma que, após reinício da
aplicação com o mesmo banco, o recurso continue consultável pelo mesmo `id`.

#### Scenario: SPEC-NFR-C1 — Persistência real de Aula

- **WHEN** uma Aula é criada via API e a aplicação reinicia com o mesmo banco
- **THEN** a Aula continua consultável pelo mesmo `id`
