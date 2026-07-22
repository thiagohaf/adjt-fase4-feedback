# Spec — inscricao-gate-avaliacao

> Deriva de `openspec/modules/03-inscricao-curso-aula/specs.md` (FR-6: SPEC-6.1–6.3).
> Este capability NÃO implementa Avaliação completa (FR-7) — apenas o gate de inscrição.
> Cada cenário DEVE virar teste.

## ADDED Requirements

### Requirement: Criar Avaliação sem inscrição na Aula é rejeitado

O sistema SHALL, em `POST /api/v1/avaliacoes` (`@RolesAllowed("ESTUDANTE")`), exigir
`aulaId` no body e verificar se o Estudante autenticado (`sub`) possui `InscricaoAula` para
essa Aula. Se não houver inscrição, SHALL responder com status de rejeição **403 Forbidden**
e `code` igual a `"INSCRICAO_AULA_OBRIGATORIA"` no envelope `{ code, message, traceId }`,
sem tratar a operação como sucesso de negócio. Ausência de `aulaId` válido SHALL mapear para
**400** `VALIDATION_ERROR`.

#### Scenario: SPEC-6.1 — Criar Avaliação sem inscrição na Aula é rejeitado

- **WHEN** o Estudante com JWT válido envia `POST /api/v1/avaliacoes` com `aulaId` de Aula na qual não está inscrito
- **THEN** a API responde 403 Forbidden com `code` igual a `"INSCRICAO_AULA_OBRIGATORIA"`
- **AND** a operação não é tratada como criação bem-sucedida de Avaliação

### Requirement: Criar Avaliação com inscrição na Aula passa o gate

O sistema SHALL permitir que `POST /api/v1/avaliacoes` continue o fluxo do stub de Avaliação
(resposta 201 provisória) quando o Estudante estiver inscrito na Aula informada. Este
requirement NÃO obriga persistência de Avaliação de domínio, cálculo de Urgência nem
demais regras de FR-7.

#### Scenario: SPEC-6.2 — Criar Avaliação com inscrição na Aula passa o gate

- **WHEN** o Estudante inscrito na Aula envia `POST /api/v1/avaliacoes` com esse `aulaId`
- **THEN** a API não rejeita por falta de inscrição
- **AND** o comportamento restante permanece o do stub de Avaliação (FR-7 fora de escopo)

### Requirement: Porta de verificação de inscrição em Aula

O sistema SHALL expor uma porta/caso de uso de application (ex.:
`VerificarInscricaoAulaUseCase` / `existsByEstudanteIdAndAulaId`) que retorna se o par
`(estudanteId, aulaId)` possui inscrição. O gate de `AvaliacaoResource` MUST usar essa
mesma porta, preparando o módulo Avaliação (AD-15 / FR-7).

#### Scenario: SPEC-6.3 — Porta de verificação disponível

- **WHEN** um caso de uso consulta se `(estudanteId, aulaId)` está inscrito
- **THEN** a porta retorna verdadeiro ou falso de forma determinística conforme `inscricao_aula`
- **AND** o gate HTTP de Avaliação usa a mesma verificação
