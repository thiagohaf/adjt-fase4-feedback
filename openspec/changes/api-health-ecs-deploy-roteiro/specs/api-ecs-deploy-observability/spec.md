## ADDED Requirements

### Requirement: API deployável em ECS Fargate atrás de ALB

A API Quarkus DEVE ser publicável como serviço ECS Fargate (sizing demo) atrás de
um Application Load Balancer internet-facing, com health check apontando para o
endpoint de readiness (FR-15, AD-1, AD-11, AD-17 demo).

#### Scenario: Serviço alcançável via ALB

- **WHEN** a stack `FeedbacksApiStack` (ou equivalente) está deployed com desiredCount ≥ 1
- **THEN** `GET http(s)://<alb-dns>/api/v1/health` retorna status saudável com DB up
- **AND** rotas `/api/v1/*` existentes continuam funcionando com o mesmo contrato

#### Scenario: Imagem versionada no ECR

- **WHEN** o pipeline ou build local publica a imagem da API
- **THEN** um repositório ECR contém a imagem usada pela task definition
- **AND** o artefato é referenciado de forma versionada (tag imutável ou digest)

### Requirement: Pipeline de deploy automatizado

Componentes atualizáveis da API DEVEM ter pipeline documentado e executável que
constrói, publica a imagem e permite deploy da stack (FR-15).

#### Scenario: Evidência de CI/CD

- **WHEN** o workflow de CI/deploy é executado com sucesso
- **THEN** há evidência filmável (Actions verde e/ou `cdk deploy` recente)
- **AND** o fluxo está documentado em README ou roteiro

### Requirement: Métricas ALB e alarme CloudWatch

O sistema DEVE expor métricas de mercado via ALB/CloudWatch (RequestCount, erros
4XX/5XX, latência/TargetResponseTime ou equivalentes) e DEVE existir ≥1 alarme
configurado e documentado (FR-14, AD-11).

#### Scenario: Métricas visíveis após tráfego

- **WHEN** a demo gera tráfego HTTP via ALB
- **THEN** é possível mostrar no CloudWatch ao menos RequestCount e indicador de erro/latência

#### Scenario: Alarme configurado

- **WHEN** a stack de API é deployed
- **THEN** existe alarme CloudWatch (ex.: 5XX do target > 0 em janela curta)
- **AND** o alarme é referenciado no roteiro/README para a gravação
