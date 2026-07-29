# Specs — Módulo 01: Autenticação e Papéis

| Campo | Valor |
| --- | --- |
| **Módulo** | `01-autenticacao-e-papeis` |
| **FRs** | FR-1 (Login JWT), FR-2 (Autorização por papel) |
| **Formato** | Given / When / Then |
| **Status** | Rascunho para revisão |

---

## FR-1 — Login e emissão de JWT

### SPEC-1.1 — Login bem-sucedido (Estudante)

**Given** que existe um usuário cadastrado com email `estudante@demo.fiap`, senha `senha123` e papel `ESTUDANTE`  
**And** o endpoint `POST /api/v1/auth/login` está disponível  
**When** o cliente envia:

```json
{
  "email": "estudante@demo.fiap",
  "password": "senha123"
}
```

**Then** a API responde **200 OK**  
**And** o corpo contém um campo `accessToken` (string JWT não vazia)  
**And** o corpo contém `tokenType` igual a `"Bearer"`  
**And** o corpo contém `expiresIn` (segundos, inteiro positivo)  
**And** o JWT decodificado contém claim `sub` com o UUID do usuário  
**And** o JWT decodificado contém claim `role` igual a `"ESTUDANTE"`  
**And** o JWT decodificado contém claim `exp` no futuro  

---

### SPEC-1.2 — Login bem-sucedido (Administrador)

**Given** que existe um usuário cadastrado com email `admin@demo.fiap`, senha `admin123` e papel `ADMINISTRADOR`  
**When** o cliente envia credenciais válidas para `POST /api/v1/auth/login`  
**Then** a API responde **200 OK**  
**And** o JWT decodificado contém claim `role` igual a `"ADMINISTRADOR"`  

---

### SPEC-1.3 — Credenciais inválidas (email inexistente)

**Given** que não existe usuário com email `naoexiste@demo.fiap`  
**When** o cliente envia login com esse email e qualquer senha  
**Then** a API responde **401 Unauthorized**  
**And** o corpo segue o envelope de erro `{ "code", "message", "traceId" }`  
**And** `code` é `"AUTH_INVALID_CREDENTIALS"`  
**And** nenhum token JWT é emitido  

---

### SPEC-1.4 — Credenciais inválidas (senha incorreta)

**Given** que existe usuário `estudante@demo.fiap` com senha `senha123`  
**When** o cliente envia login com email correto e senha `errada`  
**Then** a API responde **401 Unauthorized**  
**And** `code` é `"AUTH_INVALID_CREDENTIALS"`  
**And** nenhum token JWT é emitido  

---

### SPEC-1.5 — Payload de login inválido

**Given** o endpoint de login está disponível  
**When** o cliente envia body sem `email` ou sem `password`  
**Then** a API responde **400 Bad Request**  
**And** `code` é `"VALIDATION_ERROR"`  
**And** nenhum token JWT é emitido  

---

### SPEC-1.6 — Email com formato inválido

**Given** o endpoint de login está disponível  
**When** o cliente envia `"email": "nao-e-email"`  
**Then** a API responde **400 Bad Request**  
**And** `code` é `"VALIDATION_ERROR"`  

---

### SPEC-1.7 — Acesso a rota protegida com token válido

**Given** que o Estudante obteve JWT válido via login  
**When** o cliente chama qualquer rota protegida (ex.: `GET /api/v1/cursos`) com header `Authorization: Bearer <token>`  
**Then** a API processa a requisição (não retorna 401 por autenticação)  

---

### SPEC-1.8 — Acesso a rota protegida sem token

**Given** uma rota de negócio protegida (ex.: `GET /api/v1/cursos`)  
**When** o cliente envia request **sem** header `Authorization`  
**Then** a API responde **401 Unauthorized**  
**And** `code` é `"AUTH_MISSING_TOKEN"`  

---

### SPEC-1.9 — Acesso a rota protegida com token malformado

**Given** uma rota de negócio protegida  
**When** o cliente envia `Authorization: Bearer token-invalido`  
**Then** a API responde **401 Unauthorized**  
**And** `code` é `"AUTH_INVALID_TOKEN"`  

---

### SPEC-1.10 — Acesso a rota protegida com token expirado

**Given** um JWT emitido com `exp` no passado  
**When** o cliente usa esse token em rota protegida  
**Then** a API responde **401 Unauthorized**  
**And** `code` é `"AUTH_TOKEN_EXPIRED"`  

---

### SPEC-1.11 — Rotas públicas sem autenticação

**Given** a API está em execução  
**When** o cliente chama `POST /api/v1/auth/login` ou `GET /api/v1/health` **sem** token  
**Then** a API **não** retorna 401 por ausência de token  
**And** responde conforme a lógica da rota (200 ou erro de negócio, nunca bloqueio auth)  

---

## FR-2 — Autorização por papel

### SPEC-2.1 — Estudante não cria Curso

**Given** que o Estudante possui JWT válido com `role=ESTUDANTE`  
**When** o cliente envia `POST /api/v1/cursos` com token do Estudante  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  
**And** nenhum Curso é criado  

---

### SPEC-2.2 — Estudante não cria Aula

**Given** que o Estudante possui JWT válido com `role=ESTUDANTE`  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas` com token do Estudante  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  

---

### SPEC-2.3 — Administrador cria Curso e Aula

**Given** que o Administrador possui JWT válido com `role=ADMINISTRADOR`  
**When** o cliente envia `POST /api/v1/cursos` ou `POST /api/v1/cursos/{cursoId}/aulas` com token do Admin  
**Then** a API **não** retorna 403 por autorização  
**And** processa a criação (201 ou erro de validação de negócio, nunca 403)  

---

### SPEC-2.4 — Administrador consulta catálogo sem inscrição

**Given** que o Administrador possui JWT válido  
**And** o Administrador **não** está inscrito em nenhum Curso ou Aula  
**When** o cliente envia `GET /api/v1/cursos` ou `GET /api/v1/cursos/{id}/aulas`  
**Then** a API responde **200 OK** com a listagem/consulta  

---

### SPEC-2.5 — Estudante consulta catálogo

**Given** que o Estudante possui JWT válido  
**When** o cliente envia `GET /api/v1/cursos` ou `GET /api/v1/aulas/{id}`  
**Then** a API responde **200 OK** (ou 404 se id inexistente — nunca 403)  

---

### SPEC-2.6 — Criação de Avaliação exige papel Estudante

**Given** que o Administrador possui JWT válido com `role=ADMINISTRADOR`  
**When** o cliente envia `POST /avaliacao` com token do Admin  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  

---

### SPEC-2.7 — Estudante cria Avaliação (autorização OK)

**Given** que o Estudante possui JWT válido com `role=ESTUDANTE`  
**And** o Estudante está inscrito na Aula alvo  
**When** o cliente envia `POST /avaliacao` com token do Estudante  
**Then** a API **não** retorna 403 por autorização  

---

### SPEC-2.8 — Inscrição exige papel Estudante

**Given** que o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{id}/inscricoes` ou equivalente de inscrição  
**Then** a API responde **403 Forbidden**  

---

### SPEC-2.9 — Estudante realiza inscrição

**Given** que o Estudante possui JWT válido  
**When** o cliente envia request de inscrição em Curso/Aula  
**Then** a API **não** retorna 403 por autorização  

---

### SPEC-2.10 — Listagem de Avaliações: Admin vê todas

**Given** que existem Avaliações de múltiplos Estudantes  
**And** o Administrador possui JWT válido  
**When** o cliente envia `GET /avaliacao` com token do Admin  
**Then** a API retorna Avaliações de **todos** os Estudantes  

---

### SPEC-2.11 — Listagem de Avaliações: Estudante vê só as próprias

**Given** que existem Avaliações do Estudante A e do Estudante B  
**And** o Estudante A possui JWT válido  
**When** o cliente envia `GET /avaliacao` com token do Estudante A  
**Then** a API retorna **apenas** Avaliações criadas pelo Estudante A  
**And** nenhuma Avaliação do Estudante B é incluída  

---

## Requisitos não-funcionais (autenticação)

### SPEC-NFR-1 — Segredo JWT fora do código

**Given** o ambiente de produção (profile `aws`)  
**When** a aplicação inicia  
**Then** o segredo JWT é carregado de Secrets Manager (`jwtSecret`)  
**And** nenhum valor default de segredo está hardcoded no código-fonte  

---

### SPEC-NFR-2 — Senha nunca em log

**Given** qualquer tentativa de login (sucesso ou falha)  
**When** a requisição é processada  
**Then** logs estruturados **não** contêm o campo `password` nem o JWT completo  

---

### SPEC-NFR-3 — Senha armazenada com hash

**Given** um usuário persistido no banco  
**When** o registro é consultado  
**Then** o campo de senha contém hash BCrypt (nunca plaintext)  

---

## Mapa de códigos de erro

| HTTP | code | Quando |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Payload inválido no login |
| 401 | `AUTH_INVALID_CREDENTIALS` | Email/senha incorretos |
| 401 | `AUTH_MISSING_TOKEN` | Rota protegida sem Bearer |
| 401 | `AUTH_INVALID_TOKEN` | JWT malformado ou assinatura inválida |
| 401 | `AUTH_TOKEN_EXPIRED` | JWT expirado |
| 403 | `AUTH_FORBIDDEN` | Token válido, papel insuficiente |
