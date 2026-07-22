# Spec — auth-login

> Deriva de `openspec/modules/01-autenticacao-e-papeis/specs.md` (FR-1, SPEC-1.1–1.6,
> SPEC-NFR-2, SPEC-NFR-3). Cada cenário abaixo referencia o SPEC de origem e DEVE virar teste.

## ADDED Requirements

### Requirement: Login emite JWT HS256 com claims sub, role, iat e exp

O sistema SHALL expor `POST /api/v1/auth/login` público que, dadas credenciais válidas
(email + senha verificada contra hash BCrypt), responde 200 com corpo
`{ accessToken, tokenType: "Bearer", expiresIn }`, onde `accessToken` é JWT HS256 contendo
claims `sub` (UUID do usuário), `role` (`ESTUDANTE` | `ADMINISTRADOR`), `iat` e `exp` futuro
(TTL configurável, default 86400s).

#### Scenario: SPEC-1.1 — Login bem-sucedido (Estudante)

- **WHEN** o cliente envia `{"email": "estudante@demo.fiap", "password": "senha123"}` para `POST /api/v1/auth/login` e existe usuário seed com esse email, senha e papel `ESTUDANTE`
- **THEN** a API responde 200 OK
- **AND** o corpo contém `accessToken` (string JWT não vazia), `tokenType` igual a `"Bearer"` e `expiresIn` (segundos, inteiro positivo)
- **AND** o JWT decodificado contém claim `sub` com o UUID do usuário, claim `role` igual a `"ESTUDANTE"` e claim `exp` no futuro

#### Scenario: SPEC-1.2 — Login bem-sucedido (Administrador)

- **WHEN** o cliente envia credenciais válidas de `admin@demo.fiap` (senha `admin123`, papel `ADMINISTRADOR`) para `POST /api/v1/auth/login`
- **THEN** a API responde 200 OK
- **AND** o JWT decodificado contém claim `role` igual a `"ADMINISTRADOR"`

### Requirement: Credenciais inválidas retornam 401 AUTH_INVALID_CREDENTIALS sem emitir token

O sistema SHALL responder 401 Unauthorized com envelope `{ code: "AUTH_INVALID_CREDENTIALS",
message, traceId }` quando o email não existir ou a senha não corresponder ao hash, sem emitir
JWT e sem distinguir na resposta qual dos dois casos ocorreu.

#### Scenario: SPEC-1.3 — Email inexistente

- **WHEN** o cliente envia login com email `naoexiste@demo.fiap` e qualquer senha
- **THEN** a API responde 401 Unauthorized
- **AND** o corpo segue o envelope `{ code, message, traceId }` com `code` igual a `"AUTH_INVALID_CREDENTIALS"`
- **AND** nenhum token JWT é emitido

#### Scenario: SPEC-1.4 — Senha incorreta

- **WHEN** o cliente envia login com email `estudante@demo.fiap` e senha `errada`
- **THEN** a API responde 401 Unauthorized com `code` igual a `"AUTH_INVALID_CREDENTIALS"`
- **AND** nenhum token JWT é emitido

### Requirement: Payload de login inválido retorna 400 VALIDATION_ERROR

O sistema SHALL validar o payload de login (`email` obrigatório e com formato de email;
`password` obrigatório, mínimo 6 caracteres) e responder 400 Bad Request com
`code = "VALIDATION_ERROR"` no envelope padrão quando a validação falhar, sem emitir token.

#### Scenario: SPEC-1.5 — Body sem email ou sem password

- **WHEN** o cliente envia body sem `email` ou sem `password` para `POST /api/v1/auth/login`
- **THEN** a API responde 400 Bad Request com `code` igual a `"VALIDATION_ERROR"`
- **AND** nenhum token JWT é emitido

#### Scenario: SPEC-1.6 — Email com formato inválido

- **WHEN** o cliente envia `"email": "nao-e-email"` no login
- **THEN** a API responde 400 Bad Request com `code` igual a `"VALIDATION_ERROR"`

### Requirement: Senha nunca aparece em log nem em resposta

O sistema SHALL garantir que logs estruturados de tentativas de login (sucesso ou falha) não
contenham o campo `password` nem o JWT completo, e que a senha (raw ou hash) nunca seja exposta
em respostas da API.

#### Scenario: SPEC-NFR-2 — Senha nunca em log

- **WHEN** qualquer tentativa de login (sucesso ou falha) é processada
- **THEN** os logs estruturados não contêm o campo `password` nem o JWT completo

### Requirement: Senha armazenada exclusivamente como hash BCrypt

O sistema SHALL armazenar a senha do `Usuario` exclusivamente como hash BCrypt — nunca
plaintext — tanto no schema (`V1__create_schema.sql`) quanto no seed
(`V2__seed_usuarios.sql`, usuários `estudante@demo.fiap`/`senha123` e
`admin@demo.fiap`/`admin123` com hash gerado na migration).

#### Scenario: SPEC-NFR-3 — Hash BCrypt no banco

- **WHEN** um registro de usuário persistido é consultado no banco
- **THEN** o campo de senha contém hash BCrypt (prefixo `$2a$`/`$2b$`), nunca plaintext
- **AND** a verificação de login usa `PasswordEncoder.matches` (BCrypt) contra esse hash
