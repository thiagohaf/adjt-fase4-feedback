# Spec — avaliacao-urgencia

## Purpose

Classificação de Urgência da Avaliação na API Quarkus (`feedbacks/apps/api/`):
ALTA (nota ≤ 4), MEDIA (5–7), BAIXA (≥ 8), exposta em criação e listagem.
Derivado da change `avaliacao-aula-quarkus` (módulo 04, FR-9).

## Requirements

### Requirement: Urgência ALTA para nota menor ou igual a 4

Ao persistir Avaliação, o sistema SHALL atribuir Urgência `ALTA` quando `nota` ≤ 4
(inclusive a fronteira 4).

#### Scenario: SPEC-9.1 — Nota 4 classifica ALTA

- **WHEN** o Estudante inscrito cria Avaliação com `nota` igual a `4`
- **THEN** a Avaliação persistida possui `urgencia` igual a `"ALTA"`
- **AND** a resposta 201 inclui `urgencia` igual a `"ALTA"`

#### Scenario: SPEC-9.2 — Nota 0 classifica ALTA

- **WHEN** o Estudante inscrito cria Avaliação com `nota` igual a `0`
- **THEN** a Avaliação persistida possui `urgencia` igual a `"ALTA"`

### Requirement: Urgência MEDIA para nota entre 5 e 7

Ao persistir Avaliação, o sistema SHALL atribuir Urgência `MEDIA` quando `nota` estiver no
intervalo [5, 7] inclusive (fronteiras 5 e 7).

#### Scenario: SPEC-9.3 — Nota 5 classifica MEDIA

- **WHEN** o Estudante inscrito cria Avaliação com `nota` igual a `5`
- **THEN** a Avaliação persistida possui `urgencia` igual a `"MEDIA"`

#### Scenario: SPEC-9.4 — Nota 7 classifica MEDIA

- **WHEN** o Estudante inscrito cria Avaliação com `nota` igual a `7`
- **THEN** a Avaliação persistida possui `urgencia` igual a `"MEDIA"`

### Requirement: Urgência BAIXA para nota maior ou igual a 8

Ao persistir Avaliação, o sistema SHALL atribuir Urgência `BAIXA` quando `nota` ≥ 8
(inclusive a fronteira 8).

#### Scenario: SPEC-9.5 — Nota 8 classifica BAIXA

- **WHEN** o Estudante inscrito cria Avaliação com `nota` igual a `8`
- **THEN** a Avaliação persistida possui `urgencia` igual a `"BAIXA"`

#### Scenario: SPEC-9.6 — Nota 10 classifica BAIXA

- **WHEN** o Estudante inscrito cria Avaliação com `nota` igual a `10`
- **THEN** a Avaliação persistida possui `urgencia` igual a `"BAIXA"`

### Requirement: Urgência disponível para consulta

O sistema SHALL expor o campo `urgencia` nas respostas de criação e de listagem
(`GET /api/v1/avaliacoes`), tornando-o disponível para o read model de relatório (AD-16)
e para o fluxo de alerta (FR-10, fora desta change).

#### Scenario: SPEC-9.7 — Listagem inclui urgencia

- **WHEN** existe pelo menos uma Avaliação persistida e o cliente autorizado envia `GET /api/v1/avaliacoes`
- **THEN** cada item inclui `urgencia` ∈ {`ALTA`,`MEDIA`,`BAIXA`}
- **AND** inclui `nota` e `ocorridoEm`
