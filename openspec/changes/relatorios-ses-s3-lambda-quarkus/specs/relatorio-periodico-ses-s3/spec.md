## ADDED Requirements

### Requirement: Lambda report gera Relatório semanal com agregados

O sistema SHALL, quando `periodo` = `"semanal"`, consolidar Avaliações cuja
`ocorrido_em` cai na janela dos **7 dias civis anteriores** ao dia civil de disparo
no fuso `America/Sao_Paulo`, em uma função serverless distinta da API e do alerta
(`lambda-report`, Quarkus + `quarkus-amazon-lambda`). O Relatório semanal MUST incluir
média de notas, quantidade de Avaliações **por dia civil SP** e quantidade por nível
de Urgência persistido (FR-11, AD-6, AD-13, AD-16).

#### Scenario: SPEC-11.1 — Semanal agrega média, qty/dia e qty/urgência

- **WHEN** a Lambda report é invocada com `periodo` = `"semanal"` e existem Avaliações
  na janela semanal
- **THEN** o relatório gerado contém média das notas do período
- **AND** contém quantidade de Avaliações agrupada por dia civil `America/Sao_Paulo`
- **AND** contém quantidade por urgência (`ALTA`, `MEDIA`, `BAIXA`)

#### Scenario: SPEC-11.2 — Janela semanal é civil SP (não rolling UTC)

- **WHEN** o disparo ocorre em um instante cujo dia civil em `America/Sao_Paulo` é D
- **THEN** a janela de leitura é o intervalo dos 7 dias civis imediatamente anteriores a D
  (filtro exclusivo em `ocorrido_em`)
- **AND** Avaliações fora dessa janela não entram nos agregados

### Requirement: Lambda report gera Relatório diário com agregados

O sistema SHALL, quando `periodo` = `"diario"`, consolidar Avaliações do **dia civil
anterior** em `America/Sao_Paulo` (filtro em `ocorrido_em`). O Relatório diário MUST
incluir média de notas do dia, quantidade total e quantidade por Urgência, e MUST
coexistir com o Relatório semanal (não o substitui) (FR-17, AD-6, AD-16).

#### Scenario: SPEC-17.1 — Diário agrega média, qty total e qty/urgência

- **WHEN** a Lambda report é invocada com `periodo` = `"diario"` e existem Avaliações
  no dia civil anterior (SP)
- **THEN** o relatório gerado contém média das notas desse dia
- **AND** contém quantidade total de Avaliações do dia
- **AND** contém quantidade por urgência

#### Scenario: SPEC-17.2 — Diário e semanal são modos da mesma Lambda

- **WHEN** a mesma função `lambda-report` é invocada com `periodo` = `"diario"` e, em
  outra invocação, com `periodo` = `"semanal"`
- **THEN** ambas as invocações são processadas pelo mesmo deployable
- **AND** cada uma produz agregados conforme sua janela (FR-11 e FR-17 coexistem)

### Requirement: Entrega HTML por SES e PDF no S3

O sistema SHALL, para cada Relatório diário ou semanal gerado com sucesso, enviar
e-mail HTML ao Administrador (`adminEmail`) identificando tipo e período, e SHALL
gravar o PDF correspondente no S3 sob
`relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf` (FR-12, AD-6, AD-12, AD-18).

#### Scenario: SPEC-12.1 — E-mail HTML com tipo e agregados

- **WHEN** um relatório (`diario` ou `semanal`) é gerado com sucesso
- **THEN** o adapter SES é invocado uma vez com destinatário = `adminEmail` / `ADMIN_EMAIL`
- **AND** o corpo HTML identifica o tipo (diário vs semanal), o período e os agregados

#### Scenario: SPEC-12.2 — PDF recuperável no S3

- **WHEN** um relatório é gerado com sucesso
- **THEN** um objeto PDF é gravado no bucket configurado
- **AND** a chave do objeto segue `relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf`
  com `periodo` ∈ {`diario`,`semanal`}

### Requirement: Agendamento EventBridge e invoke manual

O sistema SHALL disparar a `lambda-report` via EventBridge no horário **08:00**
`America/Sao_Paulo` (diário todos os dias; semanal às segundas) com `periodo`
correspondente, e SHALL aceitar invoke manual (console/CLI) com o mesmo payload e o
**mesmo** algoritmo de janela — sem endpoint HTTP admin de relatório (AD-6, AD-10).

#### Scenario: SPEC-12.3 — Invoke manual usa a mesma janela do cron

- **WHEN** a Lambda é invocada manualmente com `{ "periodo": "diario" }` (ou `"semanal"`)
  em um `triggerInstant` T
- **THEN** a janela calculada é idêntica à que o cron usaria para o mesmo T e `periodo`
- **AND** não existe rota HTTP da API exclusiva para gerar relatório no MVP

### Requirement: Read-only no domínio e SRP

A `lambda-report` MUST ler Avaliações apenas para agregação (sem insert/update/delete
de entidades de negócio) e MUST ser deployable separado da API e de
`lambda-notification` (AD-1, AD-3, AD-13, AD-16).

#### Scenario: SPEC-11.3 — Sem mutação de Avaliação

- **WHEN** a Lambda processa um relatório
- **THEN** nenhuma operação de escrita no domínio Avaliação (nem Curso/Aula/Inscrição)
  é executada
- **AND** o artefato empacotado é o `function.zip` do módulo `report`, distinto de
  `api` e `notification`

### Requirement: Destinatário e segredos externos

O sistema SHALL resolver destinatário SES e credenciais de leitura do banco a partir
de configuração/Secrets Manager (`adminEmail`, `dbUrl`/`dbUser`/`dbPassword`) e MUST
NOT hardcodar e-mail ou senha de produção no repositório (AD-12).

#### Scenario: SPEC-12.4 — adminEmail via configuração

- **WHEN** a Lambda inicia com `ADMIN_EMAIL` (ou secret `adminEmail`) definido
- **THEN** o e-mail HTML usa esse valor como destinatário
- **AND** o código-fonte não contém endereço de e-mail fixo de produção

### Requirement: Período vazio ainda entrega evidência

Quando não houver Avaliações na janela, o sistema SHALL ainda gerar e entregar
relatório com agregados zerados/vazios (e-mail + PDF), para demo previsível.

#### Scenario: SPEC-12.5 — Janela sem avaliações

- **WHEN** a Lambda é invocada e a janela não contém Avaliações
- **THEN** SES e S3 são invocados com relatório indicando totais zero / listas vazias
- **AND** o processamento conclui com sucesso (sem falha por “sem dados”)
