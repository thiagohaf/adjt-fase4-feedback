## ADDED Requirements

### Requirement: Roteiro único de demonstração YouTube

DEVE existir um roteiro consolidado listando cenas na ordem, o que mostrar
(Postman/console/e-mail/S3/CI), e evidência de requisito (FR-16, PRD §11), com
tempo-alvo acadêmico (~8–15 min).

#### Scenario: Cobertura das cenas obrigatórias

- **WHEN** o apresentador segue `docs/ROTEIRO-DEMO.md`
- **THEN** o roteiro cobre health, auth/fluxo, alerta, relatórios, PDF S3,
  métricas/alarme, deploy/CI e encerramento (teardown/custo)
- **AND** aponta para artefatos existentes (Postman, UJ4-ROTEIRO, infra README)
  sem contradizê-los

#### Scenario: Pré-gravação checklist

- **WHEN** o apresentador prepara a gravação
- **THEN** o roteiro inclui checklist (SES, seeds, PDF gerado, alarmes, escalas mínimas)
