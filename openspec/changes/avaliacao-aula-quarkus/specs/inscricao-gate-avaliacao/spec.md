## MODIFIED Requirements

### Requirement: Criar Avaliação com inscrição na Aula passa o gate

O sistema SHALL permitir que `POST /api/v1/avaliacoes` continue o fluxo de criação real de
Avaliação (FR-7: validar `descricao`/`nota`, persistir, derivar Urgência, aplicar unicidade)
quando o Estudante estiver inscrito na Aula informada. O gate MUST NÃO rejeitar por falta de
inscrição nesse caso. Demais regras de criação, listagem e publish são definidas pelas
capabilities `avaliacao-criar`, `avaliacao-urgencia`, `avaliacao-listar` e
`avaliacao-evento-alerta`.

#### Scenario: SPEC-6.2 — Criar Avaliação com inscrição na Aula passa o gate

- **WHEN** o Estudante inscrito na Aula envia `POST /api/v1/avaliacoes` com esse `aulaId` e payload válido de Avaliação (`descricao`, `nota`)
- **THEN** a API não rejeita por falta de inscrição
- **AND** o fluxo continua para a criação real de Avaliação (persistência + Urgência conforme FR-7/FR-9)
