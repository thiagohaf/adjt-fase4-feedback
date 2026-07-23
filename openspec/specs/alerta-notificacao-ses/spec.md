# Spec — alerta-notificacao-ses

## Purpose

Processamento assíncrono do Alerta de urgência (FR-10): Lambda Quarkus distinta da API
consome o contrato AD-5 da fila SQS e envia e-mail SES ao Administrador, sem acesso a
RDS. Derivado da change `alerta-ses-lambda-quarkus` (módulo 05).

## Requirements

### Requirement: Lambda consome evento AD-5 e envia e-mail SES

O sistema SHALL processar mensagens cujo body é o JSON UTF-8 do contrato AD-5
(`avaliacaoId`, `descricao`, `urgencia`, `ocorridoEm`, `aulaId`, `cursoId`) em uma
função serverless distinta da API (`lambda-notification`, Quarkus +
`quarkus-amazon-lambda`) e SHALL enviar um Alerta de urgência por SES ao e-mail do
Administrador contendo no mínimo descrição, urgência e data (`ocorridoEm`). A Lambda
MUST NOT consultar o banco de dados (AD-5, AD-13, FR-10).

#### Scenario: SPEC-10.1 — Payload ALTA válido dispara SES

- **WHEN** a Lambda recebe uma mensagem SQS com body AD-5 válido e `urgencia` = `"ALTA"`
- **THEN** o adapter SES é invocado uma vez com destinatário = e-mail do Administrador
  configurado (`adminEmail` / env)
- **AND** o conteúdo do e-mail inclui `descricao`, urgência `ALTA` e a data `ocorridoEm`

#### Scenario: SPEC-10.2 — Lambda não acessa RDS

- **WHEN** a Lambda processa um alerta
- **THEN** nenhuma conexão JDBC/ORM ao PostgreSQL é estabelecida
- **AND** todos os dados do e-mail vêm exclusivamente do payload da mensagem

### Requirement: Destinatário e segredo não ficam no código-fonte

O sistema SHALL resolver o destinatário do Alerta de urgência a partir de configuração
externa (variável de ambiente e/ou Secrets Manager chave `adminEmail`) e MUST NOT
hardcodar o endereço de e-mail no repositório (AD-12).

#### Scenario: SPEC-10.3 — E-mail admin via configuração

- **WHEN** a Lambda inicia com `ADMIN_EMAIL` (ou secret `adminEmail`) definido
- **THEN** o envio SES usa esse valor como destinatário
- **AND** o código-fonte da aplicação não contém um endereço de e-mail fixo de produção

### Requirement: Payload inválido não envia e-mail

O sistema SHALL rejeitar (falhar o processamento da mensagem) bodies que não satisfaçam
o contrato AD-5 (JSON inválido ou campos obrigatórios ausentes) e MUST NOT chamar SES
nesses casos. A falha MUST ser observável para retry/DLQ da fila (AD-18).

#### Scenario: SPEC-10.4 — Body inválido não chama SES

- **WHEN** a Lambda recebe uma mensagem cujo body não é JSON AD-5 válido
- **THEN** o adapter SES não é invocado
- **AND** o handler sinaliza falha de processamento (exception / batch item failure)

### Requirement: Falha de SES não reverte Avaliação

O sistema SHALL tratar falha transitória ou permanente no envio SES como problema da
borda assíncrona (retry SQS/Lambda + DLQ). A Avaliação já persistida pela API MUST
permanecer intacta — a Lambda não possui operação de delete/update de Avaliação
(AD-4, AD-18, FR-10).

#### Scenario: SPEC-10.5 — SES falha e domínio permanece

- **WHEN** o adapter SES lança erro ao enviar o Alerta de urgência
- **THEN** a Lambda propaga a falha para o mecanismo de retry/DLQ
- **AND** não existe caminho na Lambda que altere ou apague a Avaliação no banco

### Requirement: Processamento em função serverless SRP

O Alerta de urgência MUST ser processado em deployable separado da API REST
(`lambda-notification`), com responsabilidade única de notificação (não gerar relatório
nem expor HTTP de negócio) (FR-10, AD-1, AD-13).

#### Scenario: SPEC-10.6 — Deployable distinto da API

- **WHEN** o artefato de alerta é empacotado
- **THEN** o resultado é o packaging Lambda Quarkus (`function.zip` ou equivalente)
  do módulo `notification`
- **AND** o módulo não embute servidor HTTP de API de negócio nem lógica de relatório
