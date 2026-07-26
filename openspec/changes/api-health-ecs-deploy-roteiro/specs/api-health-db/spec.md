## ADDED Requirements

### Requirement: Health público reflete prontidão da API e do banco

A API DEVE expor `GET /api/v1/health` (público, AD-9) retornando status UP somente
quando a aplicação e a conectividade básica com o PostgreSQL estiverem saudáveis.
Falha de DB DEVE refletir status não-UP de forma documentada (FR-13, AD-11).

#### Scenario: API e DB saudáveis

- **WHEN** a API está no ar e o datasource responde a uma checagem básica
- **THEN** `GET /api/v1/health` retorna HTTP 200 com indicação de status UP

#### Scenario: Banco inacessível

- **WHEN** a checagem de conectividade com o PostgreSQL falha
- **THEN** `GET /api/v1/health` (e/ou readiness SmallRye) NÃO reporta UP saudável
- **AND** a falha é observável na resposta de health documentada para a demo

### Requirement: SmallRye Health para probes

A API DEVE expor SmallRye Health em `/q/health` (e readiness em path compatível
com health check do ALB, tipicamente `/q/health/ready`) incluindo checagem de DB
(AD-11).

#### Scenario: Probe de readiness com DB ok

- **WHEN** um load balancer ou operador consulta o endpoint de readiness SmallRye
- **AND** o DB está acessível
- **THEN** a resposta indica readiness UP

#### Scenario: Probe de readiness com DB down

- **WHEN** o DB está inacessível
- **THEN** o endpoint de readiness NÃO indica UP
