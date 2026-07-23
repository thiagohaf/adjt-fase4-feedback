# Spec — avaliacao-evento-alerta

## Purpose

Publicação pós-commit de evento de alerta para Avaliação com Urgência ALTA via porta
`EvaluationEventPublisher` (contrato AD-5); adapters Kafka/SQS/fake; falha de publish
não reverte Avaliação. Lambda SES (FR-10) fora de escopo desta capability.
Derivado da change `avaliacao-aula-quarkus` (módulo 04, prep. FR-10).

## Requirements

### Requirement: Após commit, Urgência ALTA publica evento de alerta

O sistema SHALL, **após** o commit bem-sucedido de uma Avaliação com Urgência `ALTA`,
publicar um evento via porta `EvaluationEventPublisher` com o contrato JSON único (AD-5):
`avaliacaoId`, `descricao`, `urgencia`, `ocorridoEm` (ISO-8601 com offset), `aulaId`,
`cursoId`. A publicação MUST ocorrer fora da transação de escrita da Avaliação (AD-4).

#### Scenario: SPEC-10prep.1 — Avaliação ALTA dispara publish

- **WHEN** o Estudante inscrito cria Avaliação com `nota` ≤ 4 e a persistência confirma
- **THEN** `EvaluationEventPublisher.publish` é invocado uma vez com o payload AD-5 correspondente
- **AND** a Avaliação permanece persistida independentemente do resultado do publish

### Requirement: MÉDIA e BAIXA não publicam evento de alerta

O sistema SHALL NÃO invocar `EvaluationEventPublisher` quando a Urgência da Avaliação
criada for `MEDIA` ou `BAIXA`.

#### Scenario: SPEC-10prep.2 — Nota MÉDIA não publica

- **WHEN** o Estudante inscrito cria Avaliação com `nota` no intervalo [5, 7]
- **THEN** `EvaluationEventPublisher.publish` não é invocado

#### Scenario: SPEC-10prep.3 — Nota BAIXA não publica

- **WHEN** o Estudante inscrito cria Avaliação com `nota` ≥ 8
- **THEN** `EvaluationEventPublisher.publish` não é invocado

### Requirement: Falha de publish não reverte Avaliação

O sistema SHALL garantir que falha transitória ou permanente no adapter de publish
(Kafka/SQS/fake) NÃO apague nem altere a Avaliação já commitada (AD-4, AD-18).

#### Scenario: SPEC-10prep.4 — Publish falha e Avaliação permanece

- **WHEN** a Avaliação ALTA foi commitada e o publisher lança erro
- **THEN** a Avaliação continua recuperável via `GET /api/v1/avaliacoes`
- **AND** a resposta HTTP de criação permanece 201 (sucesso de domínio)

### Requirement: Contrato do evento é único entre ambientes

O sistema SHALL serializar o mesmo JSON UTF-8 independentemente do adapter (`%local` Kafka
ou `%aws` SQS), conforme AD-5 e AD-7. A Lambda de SES (FR-10) fica fora desta change.

#### Scenario: SPEC-10prep.5 — Payload contém campos obrigatórios

- **WHEN** um evento ALTA é publicado
- **THEN** o JSON contém exatamente os campos `avaliacaoId`, `descricao`, `urgencia`, `ocorridoEm`, `aulaId`, `cursoId`
- **AND** `urgencia` é `"ALTA"`
