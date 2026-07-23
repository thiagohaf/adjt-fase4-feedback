# Spec — avaliacao-listar

## Purpose

Listagem enriquecida de Avaliações na API Quarkus (`feedbacks/apps/api/`):
`GET /api/v1/avaliacoes` com `nota`/`urgencia`/`ocorridoEm`/vínculos de catálogo,
escopo Admin (todas) vs Estudante (próprias). Derivado da change
`avaliacao-aula-quarkus` (módulo 04, FR-8).

## Requirements

### Requirement: Listagem inclui nota, Urgência, data e vínculos de catálogo

O sistema SHALL, em `GET /api/v1/avaliacoes`
(`@RolesAllowed({"ESTUDANTE","ADMINISTRADOR"})`), retornar para cada Avaliação os campos
`id`, `estudanteId`, `aulaId`, `cursoId`, `descricao`, `nota`, `urgencia` e `ocorridoEm`
(ISO-8601 com offset ou Z).

#### Scenario: SPEC-8.1 — Admin lista Avaliações com campos de domínio

- **WHEN** existem Avaliações persistidas e o Administrador envia `GET /api/v1/avaliacoes`
- **THEN** a API responde 200 OK
- **AND** cada item contém `nota`, `urgencia`, `ocorridoEm`, `aulaId` e `cursoId`

### Requirement: Escopo por papel permanece (Admin todas / Estudante próprias)

O sistema SHALL manter a regra AD-8 / SPEC-2.10–2.11: Administrador vê Avaliações de todos
os Estudantes; Estudante vê apenas as próprias (`estudanteId` = `sub`).

#### Scenario: SPEC-8.2 — Estudante não vê Avaliações alheias

- **WHEN** existem Avaliações do Estudante A e do Estudante B e o cliente envia `GET /api/v1/avaliacoes` com JWT do Estudante A
- **THEN** a resposta contém apenas Avaliações com `estudanteId` igual ao `sub` de A
- **AND** nenhuma Avaliação de B é incluída

#### Scenario: SPEC-8.3 — Admin vê Avaliações de múltiplos Estudantes

- **WHEN** existem Avaliações de múltiplos Estudantes e o Administrador envia `GET /api/v1/avaliacoes`
- **THEN** a resposta inclui Avaliações de todos esses Estudantes

### Requirement: Listagem exige autenticação e papel autorizado

O sistema SHALL rejeitar `GET /api/v1/avaliacoes` sem token com **401** `AUTH_*` e MUST
não expor a rota a papéis além de `ESTUDANTE` e `ADMINISTRADOR`.

#### Scenario: SPEC-8.4 — Sem token na listagem

- **WHEN** o cliente envia `GET /api/v1/avaliacoes` sem `Authorization`
- **THEN** a API responde 401 com código `AUTH_MISSING_TOKEN` (ou equivalente do módulo 01)
