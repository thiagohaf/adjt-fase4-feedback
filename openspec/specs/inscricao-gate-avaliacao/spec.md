# Spec — inscricao-gate-avaliacao

## Purpose

Gate FR-6 em `POST /avaliacao`: exige inscrição do Estudante na Aula
(`INSCRICAO_AULA_OBRIGATORIA` / `VALIDATION_ERROR`) via porta
`VerificarInscricaoAulaUseCase`. Com inscrição, o fluxo segue para criação real
de Avaliação (FR-7/FR-9). Derivado das changes `inscricao-curso-aula-quarkus`
(módulo 03) e `avaliacao-aula-quarkus` (módulo 04).

## Requirements

### Requirement: Criar Avaliação sem inscrição na Aula é rejeitado

O sistema SHALL, em `POST /avaliacao` (`@RolesAllowed("ESTUDANTE")`), exigir
`aulaId` no body e verificar se o Estudante autenticado (`sub`) possui `InscricaoAula` para
essa Aula. Se não houver inscrição, SHALL responder com status de rejeição **403 Forbidden**
e `code` igual a `"INSCRICAO_AULA_OBRIGATORIA"` no envelope `{ code, message, traceId }`,
sem tratar a operação como sucesso de negócio. Ausência de `aulaId` válido SHALL mapear para
**400** `VALIDATION_ERROR`.

#### Scenario: SPEC-6.1 — Criar Avaliação sem inscrição na Aula é rejeitado

- **WHEN** o Estudante com JWT válido envia `POST /avaliacao` com `aulaId` de Aula na qual não está inscrito
- **THEN** a API responde 403 Forbidden com `code` igual a `"INSCRICAO_AULA_OBRIGATORIA"`
- **AND** a operação não é tratada como criação bem-sucedida de Avaliação

### Requirement: Criar Avaliação com inscrição na Aula passa o gate

O sistema SHALL permitir que `POST /avaliacao` continue o fluxo de criação real de
Avaliação (FR-7: validar `descricao`/`nota`, persistir, derivar Urgência, aplicar unicidade)
quando o Estudante estiver inscrito na Aula informada. O gate MUST NÃO rejeitar por falta de
inscrição nesse caso. Demais regras de criação, listagem e publish são definidas pelas
capabilities `avaliacao-criar`, `avaliacao-urgencia`, `avaliacao-listar` e
`avaliacao-evento-alerta`.

#### Scenario: SPEC-6.2 — Criar Avaliação com inscrição na Aula passa o gate

- **WHEN** o Estudante inscrito na Aula envia `POST /avaliacao` com esse `aulaId` e payload válido de Avaliação (`descricao`, `nota`)
- **THEN** a API não rejeita por falta de inscrição
- **AND** o fluxo continua para a criação real de Avaliação (persistência + Urgência conforme FR-7/FR-9)

### Requirement: Porta de verificação de inscrição em Aula

O sistema SHALL expor uma porta/caso de uso de application (ex.:
`VerificarInscricaoAulaUseCase` / `existsByEstudanteIdAndAulaId`) que retorna se o par
`(estudanteId, aulaId)` possui inscrição. O gate de `AvaliacaoResource` MUST usar essa
mesma porta, preparando o módulo Avaliação (AD-15 / FR-7).

#### Scenario: SPEC-6.3 — Porta de verificação disponível

- **WHEN** um caso de uso consulta se `(estudanteId, aulaId)` está inscrito
- **THEN** a porta retorna verdadeiro ou falso de forma determinística conforme `inscricao_aula`
- **AND** o gate HTTP de Avaliação usa a mesma verificação
