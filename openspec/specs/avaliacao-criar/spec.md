# Spec — avaliacao-criar

## Purpose

Criação de Avaliação de Aula na API Quarkus (`feedbacks/apps/api/`):
`POST /avaliacao` com `descricao`/`nota`, unicidade Estudante+Aula,
códigos `VALIDATION_ERROR` / `AULA_NOT_FOUND` / `AVALIACAO_DUPLICADA` / `AUTH_*`,
papel `ESTUDANTE`. Derivado da change `avaliacao-aula-quarkus` (módulo 04, FR-7).

## Requirements

### Requirement: Estudante inscrito cria Avaliação com descricao e nota

O sistema SHALL, em `POST /avaliacao` (`@RolesAllowed("ESTUDANTE")`), aceitar body
JSON com `aulaId` (UUID), `descricao` (texto não vazio) e `nota` (inteiro 0–10 inclusive).
O `estudanteId` MUST ser o `sub` do JWT (`CurrentUserProvider`), nunca do body. Após passar
o gate de inscrição na Aula (FR-6), o sistema SHALL persistir a Avaliação e responder
**201 Created** com `id`, `aulaId`, `cursoId`, `estudanteId`, `descricao`, `nota`,
`urgencia` e `ocorridoEm`.

#### Scenario: SPEC-7.1 — Criar Avaliação com sucesso

- **WHEN** o Estudante inscrito na Aula envia `POST /avaliacao` com `aulaId`, `descricao` e `nota` válidos (0–10)
- **THEN** a API responde 201 Created
- **AND** o corpo contém `id` (UUID), `aulaId`, `cursoId` da Aula, `estudanteId` igual ao `sub`, `descricao`, `nota`, `urgencia` e `ocorridoEm`
- **AND** a Avaliação fica persistida e recuperável via `GET /avaliacao`

### Requirement: Nota fora de 0–10 é rejeitada

O sistema SHALL rejeitar criação quando `nota` estiver ausente, não for inteiro ou estiver
fora do intervalo [0, 10], respondendo **400** com `code` igual a `"VALIDATION_ERROR"` no
envelope `{ code, message, traceId }`, sem persistir Avaliação.

#### Scenario: SPEC-7.2 — Nota inválida retorna 400 VALIDATION_ERROR

- **WHEN** o Estudante inscrito envia `POST /avaliacao` com `nota` igual a `-1` ou `11` (ou ausente)
- **THEN** a API responde 400 com `code` igual a `"VALIDATION_ERROR"`
- **AND** nenhuma Avaliação é criada

### Requirement: Descricao obrigatória

O sistema SHALL exigir `descricao` não vazia; ausência ou blank SHALL mapear para **400**
`VALIDATION_ERROR` sem persistir.

#### Scenario: SPEC-7.3 — Descricao vazia retorna 400 VALIDATION_ERROR

- **WHEN** o Estudante inscrito envia `POST /avaliacao` com `descricao` em branco ou ausente
- **THEN** a API responde 400 com `code` igual a `"VALIDATION_ERROR"`
- **AND** nenhuma Avaliação é criada

### Requirement: Aula inexistente retorna 404 AULA_NOT_FOUND

O sistema SHALL, após o gate de inscrição (ou em conjunto com resolução da Aula), rejeitar
`aulaId` inexistente com **404** e `code` igual a `"AULA_NOT_FOUND"`.

#### Scenario: SPEC-7.4 — Aula inexistente

- **WHEN** o Estudante envia `POST /avaliacao` com `aulaId` que não existe
- **THEN** a API responde 404 com `code` igual a `"AULA_NOT_FOUND"`
- **AND** nenhuma Avaliação é criada

### Requirement: Duplicata Estudante+Aula retorna 409 AVALIACAO_DUPLICADA

O sistema SHALL garantir no máximo uma Avaliação por par `(estudanteId, aulaId)`. Segunda
tentativa MUST responder **409 Conflict** com `code` igual a `"AVALIACAO_DUPLICADA"`, sem
upsert nem alteração da Avaliação existente (AD-15).

#### Scenario: SPEC-7.5 — Segunda Avaliação da mesma Aula é conflito

- **WHEN** o Estudante já possui Avaliação para a Aula e envia novamente `POST /avaliacao` com o mesmo `aulaId`
- **THEN** a API responde 409 Conflict com `code` igual a `"AVALIACAO_DUPLICADA"`
- **AND** a Avaliação original permanece inalterada

### Requirement: Administrador não cria Avaliação

O sistema SHALL manter `@RolesAllowed("ESTUDANTE")` em `POST /avaliacao`; token de
`ADMINISTRADOR` MUST receber **403** `AUTH_FORBIDDEN` (contrato módulo 01 / SPEC-2.6).

#### Scenario: SPEC-7.6 — Admin recebe AUTH_FORBIDDEN

- **WHEN** o cliente envia `POST /avaliacao` com JWT de `role=ADMINISTRADOR`
- **THEN** a API responde 403 com `code` igual a `"AUTH_FORBIDDEN"`

### Requirement: Criação exige autenticação

O sistema SHALL rejeitar `POST /avaliacao` sem Bearer token válido com **401** e
código `AUTH_*` do módulo 01.

#### Scenario: SPEC-7.7 — Sem token

- **WHEN** o cliente envia `POST /avaliacao` sem `Authorization`
- **THEN** a API responde 401 com código `AUTH_MISSING_TOKEN` (ou equivalente do módulo 01)
