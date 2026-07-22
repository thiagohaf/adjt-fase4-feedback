# Specs — Módulo 02: Catálogo Curso/Aula

| Campo | Valor |
| --- | --- |
| **Módulo** | `02-catalogo-curso-aula` |
| **FRs** | FR-3 (criar), FR-4 (listar/consultar) |
| **Formato** | Given / When / Then |
| **Status** | Rascunho para revisão |

---

## FR-3 — Administrador cria Curso e Aula

### SPEC-3.1 — Criar Curso com sucesso

**Given** que o Administrador possui JWT válido com `role=ADMINISTRADOR`  
**When** o cliente envia `POST /api/v1/cursos` com:

```json
{
  "nome": "Arquitetura Cloud",
  "descricao": "Curso introdutório"
}
```

**Then** a API responde **201 Created**  
**And** o corpo contém `id` (UUID não nulo)  
**And** o corpo contém `nome` igual a `"Arquitetura Cloud"`  
**And** o corpo contém `descricao` igual a `"Curso introdutório"`  
**And** o Curso fica disponível em `GET /api/v1/cursos` e `GET /api/v1/cursos/{id}`  

---

### SPEC-3.2 — Criar Curso só com nome (descrição omitida)

**Given** que o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos` com `{ "nome": "Somente Nome" }`  
**Then** a API responde **201 Created**  
**And** `nome` é `"Somente Nome"`  
**And** `descricao` é `null` ou ausente de forma estável documentada no design  

---

### SPEC-3.3 — Criar Curso sem nome

**Given** que o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos` sem `nome`, com `nome` em branco ou só espaços  
**Then** a API responde **400 Bad Request**  
**And** o envelope é `{ "code", "message", "traceId" }`  
**And** `code` é `"VALIDATION_ERROR"`  
**And** nenhum Curso é criado  

---

### SPEC-3.4 — Estudante não cria Curso

**Given** que o Estudante possui JWT válido com `role=ESTUDANTE`  
**When** o cliente envia `POST /api/v1/cursos`  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  

---

### SPEC-3.5 — Criar Aula com sucesso

**Given** que existe um Curso com id `cursoId`  
**And** o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas` com:

```json
{
  "nome": "Aula 1 — Containers",
  "descricao": "Docker e Kubernetes"
}
```

**Then** a API responde **201 Created**  
**And** o corpo contém `id` (UUID), `cursoId` igual ao path, `nome` e `descricao`  
**And** o JSON **não** usa o campo `titulo`  
**And** a Aula fica disponível em `GET /api/v1/cursos/{cursoId}/aulas` e `GET /api/v1/aulas/{id}`  

---

### SPEC-3.6 — Criar Aula só com nome

**Given** que existe um Curso  
**And** o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas` com `{ "nome": "Aula mínima" }`  
**Then** a API responde **201 Created**  
**And** `descricao` é `null` ou ausente de forma estável  

---

### SPEC-3.7 — Criar Aula sem nome

**Given** que existe um Curso  
**And** o Administrador possui JWT válido  
**When** o cliente envia criação de Aula sem `nome` válido  
**Then** a API responde **400 Bad Request**  
**And** `code` é `"VALIDATION_ERROR"`  

---

### SPEC-3.8 — Criar Aula com Curso inexistente

**Given** que não existe Curso com o `cursoId` informado  
**And** o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas` com `nome` válido  
**Then** a API responde **404 Not Found**  
**And** `code` é `"CURSO_NOT_FOUND"`  
**And** nenhuma Aula é criada  

---

### SPEC-3.9 — Estudante não cria Aula

**Given** que o Estudante possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas`  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  

---

## FR-4 — Listagem e consulta de catálogo

### SPEC-4.1 — Listar Cursos (Admin e Estudante)

**Given** que existem zero ou mais Cursos persistidos  
**And** o cliente possui JWT de `ESTUDANTE` ou `ADMINISTRADOR`  
**When** o cliente envia `GET /api/v1/cursos`  
**Then** a API responde **200 OK**  
**And** o corpo é um array JSON  
**And** cada item contém pelo menos `id` e `nome`  

---

### SPEC-4.2 — Consultar Curso por id

**Given** que existe um Curso com id conhecido  
**When** o cliente autenticado (Estudante ou Admin) envia `GET /api/v1/cursos/{id}`  
**Then** a API responde **200 OK**  
**And** o corpo contém `id`, `nome` e `descricao` (nullable)  

---

### SPEC-4.3 — Consultar Curso inexistente

**Given** que não existe Curso com o id informado  
**When** o cliente autenticado envia `GET /api/v1/cursos/{id}`  
**Then** a API responde **404 Not Found**  
**And** `code` é `"CURSO_NOT_FOUND"`  

---

### SPEC-4.4 — Listar Aulas de um Curso

**Given** que existe um Curso com zero ou mais Aulas  
**When** o cliente autenticado envia `GET /api/v1/cursos/{cursoId}/aulas`  
**Then** a API responde **200 OK**  
**And** o corpo é um array; cada item contém `id`, `cursoId`, `nome`  

---

### SPEC-4.5 — Listar Aulas de Curso inexistente

**Given** que não existe Curso com o `cursoId` informado  
**When** o cliente autenticado envia `GET /api/v1/cursos/{cursoId}/aulas`  
**Then** a API responde **404 Not Found**  
**And** `code` é `"CURSO_NOT_FOUND"`  

---

### SPEC-4.6 — Consultar Aula por id

**Given** que existe uma Aula com id conhecido  
**When** o cliente autenticado envia `GET /api/v1/aulas/{id}`  
**Then** a API responde **200 OK**  
**And** o corpo contém `id`, `cursoId`, `nome` e `descricao` (nullable)  
**And** o JSON **não** usa `titulo`  

---

### SPEC-4.7 — Consultar Aula inexistente

**Given** que não existe Aula com o id informado  
**When** o cliente autenticado envia `GET /api/v1/aulas/{id}`  
**Then** a API responde **404 Not Found**  
**And** `code` é `"AULA_NOT_FOUND"`  

---

### SPEC-4.8 — Catálogo exige autenticação

**Given** uma rota de catálogo (`GET` ou `POST` de cursos/aulas)  
**When** o cliente envia a request **sem** header `Authorization`  
**Then** a API responde **401 Unauthorized**  
**And** `code` é `"AUTH_MISSING_TOKEN"`  

> Comportamento já coberto pelo módulo 01; regressão mínima neste módulo.

---

## Requisitos não-funcionais (catálogo)

### SPEC-NFR-C1 — Persistência real

**Given** um Curso ou Aula criado via API  
**When** a aplicação reinicia (mesmo banco)  
**Then** o recurso continua consultável pelo mesmo `id`  

---

### SPEC-NFR-C2 — Envelope de erro estável

**Given** qualquer erro de validação ou não encontrado do catálogo  
**When** a API responde 4xx  
**Then** o corpo segue `{ "code", "message", "traceId" }`  
**And** códigos `AUTH_*` do módulo 01 **não** são alterados  

---

### SPEC-NFR-C3 — Cobertura

**Given** o código de produção do módulo catálogo  
**When** a suíte de testes roda com JaCoCo  
**Then** a cobertura de linhas do código deste módulo é ≥ 90% (AD-14)  

---

## Mapa de códigos de erro (este módulo)

| HTTP | code | Quando |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `nome` ausente/em branco; payload inválido |
| 401 | `AUTH_MISSING_TOKEN` / `AUTH_*` | Herdados do módulo 01 — sem mudança |
| 403 | `AUTH_FORBIDDEN` | Papel insuficiente (ex.: Estudante criando) |
| 404 | `CURSO_NOT_FOUND` | Curso id inexistente (consulta, listar aulas, criar aula) |
| 404 | `AULA_NOT_FOUND` | Aula id inexistente |

> Códigos novos só de domínio de catálogo (`CURSO_*`, `AULA_*`). Não reutilizar nem renomear `AUTH_*`.
