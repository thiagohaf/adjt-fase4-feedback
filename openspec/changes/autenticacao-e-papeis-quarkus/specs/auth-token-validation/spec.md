# Spec — auth-token-validation

> Deriva de `openspec/modules/01-autenticacao-e-papeis/specs.md` (FR-1, SPEC-1.7–1.11,
> SPEC-NFR-1). Validação via `quarkus-smallrye-jwt` com HS256 explícito, chave simétrica JWK e
> `quarkus.http.auth.proactive=false` (design.md §7.1) — obrigatório para 401 com envelope
> `{ code, message, traceId }`.

## ADDED Requirements

### Requirement: Rotas protegidas aceitam Bearer token válido

O sistema SHALL validar automaticamente assinatura HS256 e expiração do Bearer token nas rotas
protegidas (extensão SmallRye JWT), populando o `SecurityIdentity` com principal (`sub`) e roles
derivadas da claim `role` (`smallrye.jwt.path.groups=role`, sem prefixo `ROLE_`, sem verificação
de `iss`).

#### Scenario: SPEC-1.7 — Token válido em rota protegida

- **WHEN** o cliente chama uma rota protegida (ex.: `GET /api/v1/cursos`) com header `Authorization: Bearer <JWT válido obtido no login>`
- **THEN** a API processa a requisição (não retorna 401 por autenticação)

### Requirement: Rota protegida sem token retorna 401 AUTH_MISSING_TOKEN

O sistema SHALL responder 401 Unauthorized com envelope `{ code: "AUTH_MISSING_TOKEN", message,
traceId }` quando uma rota protegida for chamada sem header `Authorization`. Requer
`quarkus.http.auth.proactive=false` para que o `ExceptionMapper<UnauthorizedException>` produza o
envelope.

#### Scenario: SPEC-1.8 — Sem header Authorization

- **WHEN** o cliente envia request a rota protegida sem header `Authorization`
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_MISSING_TOKEN"` no envelope `{ code, message, traceId }`

### Requirement: Token malformado ou de assinatura inválida retorna 401 AUTH_INVALID_TOKEN

O sistema SHALL responder 401 Unauthorized com `code = "AUTH_INVALID_TOKEN"` quando o Bearer
token for malformado ou tiver assinatura inválida (mapeado de
`AuthenticationFailedException`, distinguindo da expiração pela causa raiz da validação
SmallRye).

#### Scenario: SPEC-1.9 — Token malformado

- **WHEN** o cliente envia `Authorization: Bearer token-invalido` a uma rota protegida
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_INVALID_TOKEN"`

#### Scenario: Assinatura inválida (secret divergente)

- **WHEN** o cliente envia um JWT estruturalmente válido porém assinado com outro segredo
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_INVALID_TOKEN"`

### Requirement: Token expirado retorna 401 AUTH_TOKEN_EXPIRED

O sistema SHALL responder 401 Unauthorized com `code = "AUTH_TOKEN_EXPIRED"` quando o Bearer
token tiver claim `exp` no passado, distinguindo da falha genérica de token inválido
(SPEC-1.9 vs SPEC-1.10).

#### Scenario: SPEC-1.10 — Token expirado

- **WHEN** o cliente usa em rota protegida um JWT emitido com `exp` no passado (assinado com o segredo correto)
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_TOKEN_EXPIRED"`

### Requirement: Login e health permanecem públicos

O sistema SHALL manter `POST /api/v1/auth/login` e o health check (`GET /api/v1/health` /
SmallRye Health) acessíveis sem token, nunca respondendo 401 por ausência de credencial nessas
rotas (AD-9).

#### Scenario: SPEC-1.11 — Rotas públicas sem autenticação

- **WHEN** o cliente chama `POST /api/v1/auth/login` ou `GET /api/v1/health` sem token
- **THEN** a API não retorna 401 por ausência de token
- **AND** responde conforme a lógica da rota (200 ou erro de negócio/validação, nunca bloqueio de auth)

### Requirement: Segredo JWT fora do código com fonte única jwtSecret

O sistema SHALL carregar o segredo JWT de configuração externa (env `JWT_SECRET` no profile
`%local`; Secrets Manager chave `jwtSecret` no profile `%aws` — AD-12), sem valor de segredo de
produção hardcoded. Emissão (`app.jwt.secret`) e validação (JWK simétrico de
`smallrye.jwt.verify.secretkey`) MUST resolver para o mesmo valor em todos os profiles — o
wrapping em JWK `{"kty":"oct","k":"<base64url(secret)>"}` é detalhe de bootstrap derivado de
`app.jwt.secret`, não um segundo secret.

#### Scenario: SPEC-NFR-1 — Secret externo no profile aws

- **WHEN** a aplicação inicia com profile `aws`
- **THEN** o segredo JWT é resolvido de `${JWT_SECRET}` (injetado de Secrets Manager, chave `jwtSecret`)
- **AND** nenhum valor default de segredo está presente na configuração do profile `aws`

#### Scenario: Fonte única entre emissão e validação

- **WHEN** um JWT é emitido pelo `TokenService` e em seguida usado em uma rota protegida no mesmo profile
- **THEN** a validação SmallRye aceita o token (emissão e validação derivam do mesmo `jwtSecret`)

### Requirement: Toda resposta de erro carrega traceId correlacionável

O sistema SHALL gerar (ou propagar de `X-Trace-Id`) um `traceId` por request via
`ContainerRequestFilter`, armazená-lo em MDC e incluí-lo em todo `ApiErrorResponse` e nos logs.

#### Scenario: traceId presente no envelope de erro

- **WHEN** qualquer resposta de erro (400/401/403) é produzida
- **THEN** o corpo contém `traceId` não vazio

#### Scenario: X-Trace-Id propagado

- **WHEN** o cliente envia header `X-Trace-Id` em uma request que resulta em erro
- **THEN** o `traceId` do envelope de erro é igual ao valor enviado
