# Specs — Módulo 03: Inscrição Curso/Aula

| Campo | Valor |
| --- | --- |
| **Módulo** | `03-inscricao-curso-aula` |
| **FRs** | FR-5 (inscrição Curso/Aula), FR-6 (Avaliação exige inscrição) |
| **Formato** | Given / When / Then |
| **Status** | Rascunho para revisão |

---

## FR-5 — Inscrição em Curso e Aula

### SPEC-5.1 — Inscrever em Curso com sucesso

**Given** que existe um Curso com id `cursoId`  
**And** o Estudante possui JWT válido com `role=ESTUDANTE`  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/inscricoes`  
**Then** a API responde **201 Created**  
**And** o corpo contém `id` (UUID), `cursoId` igual ao path e `estudanteId` igual ao `sub` do JWT  
**And** uma segunda inscrição idêntica **não** é criada (ver SPEC-5.3)  

---

### SPEC-5.2 — Inscrever em Curso inexistente

**Given** que não existe Curso com o `cursoId` informado  
**And** o Estudante possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/inscricoes`  
**Then** a API responde **404 Not Found**  
**And** `code` é `"CURSO_NOT_FOUND"`  
**And** nenhuma inscrição é criada  

---

### SPEC-5.3 — Duplicata de inscrição em Curso

**Given** que o Estudante já está inscrito no Curso `cursoId`  
**When** o cliente envia novamente `POST /api/v1/cursos/{cursoId}/inscricoes`  
**Then** a API responde **409 Conflict**  
**And** `code` é `"INSCRICAO_DUPLICADA"`  
**And** o envelope é `{ "code", "message", "traceId" }`  

---

### SPEC-5.4 — Administrador não se inscreve em Curso

**Given** que o Administrador possui JWT válido com `role=ADMINISTRADOR`  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/inscricoes`  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  

---

### SPEC-5.5 — Inscrever em Aula com sucesso

**Given** que existe um Curso `cursoId` com Aula `aulaId` pertencente a ele  
**And** o Estudante já está inscrito nesse Curso  
**And** o Estudante possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`  
**Then** a API responde **201 Created**  
**And** o corpo contém `id` (UUID), `aulaId`, `cursoId` e `estudanteId` (sub do JWT)  

---

### SPEC-5.6 — Inscrever em Aula sem inscrição no Curso

**Given** que existem Curso e Aula válidos  
**And** o Estudante **não** está inscrito no Curso  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`  
**Then** a API responde **409 Conflict**  
**And** `code` é `"INSCRICAO_CURSO_OBRIGATORIA"`  
**And** nenhuma inscrição em Aula é criada  

---

### SPEC-5.7 — Duplicata de inscrição em Aula

**Given** que o Estudante já está inscrito na Aula `aulaId`  
**When** o cliente envia novamente `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`  
**Then** a API responde **409 Conflict**  
**And** `code` é `"INSCRICAO_DUPLICADA"`  

---

### SPEC-5.8 — Inscrever em Aula inexistente ou de outro Curso

**Given** que o `aulaId` não existe **ou** a Aula não pertence ao `cursoId` do path  
**And** o Estudante está inscrito no Curso do path (quando o Curso existe)  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`  
**Then** a API responde **404 Not Found**  
**And** `code` é `"AULA_NOT_FOUND"`  

---

### SPEC-5.9 — Inscrever em Aula com Curso inexistente

**Given** que não existe Curso com o `cursoId` do path  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`  
**Then** a API responde **404 Not Found**  
**And** `code` é `"CURSO_NOT_FOUND"`  

---

### SPEC-5.10 — Administrador não se inscreve em Aula

**Given** que o Administrador possui JWT válido  
**When** o cliente envia `POST /api/v1/cursos/{cursoId}/aulas/{aulaId}/inscricoes`  
**Then** a API responde **403 Forbidden**  
**And** `code` é `"AUTH_FORBIDDEN"`  

---

### SPEC-5.11 — Inscrição exige autenticação

**Given** que o cliente **não** envia JWT  
**When** o cliente envia qualquer `POST .../inscricoes`  
**Then** a API responde **401 Unauthorized**  
**And** `code` é um dos `AUTH_*` já definidos no módulo 01  

---

## FR-6 — Avaliação exige inscrição

### SPEC-6.1 — Criar Avaliação sem inscrição na Aula é rejeitado

**Given** que o Estudante possui JWT válido  
**And** o Estudante **não** está inscrito na Aula referenciada no request  
**When** o cliente envia `POST /avaliacao` com `aulaId` (ou equivalente)  
**Then** a API **rejeita** a criação  
**And** `code` é `"INSCRICAO_AULA_OBRIGATORIA"`  
**And** nenhuma Avaliação de domínio é criada (stub não avança como sucesso de negócio)  

---

### SPEC-6.2 — Criar Avaliação com inscrição na Aula passa o gate

**Given** que o Estudante está inscrito na Aula alvo (e, por consequência, no Curso)  
**And** o Estudante possui JWT válido  
**When** o cliente envia `POST /avaliacao` com `aulaId` válido  
**Then** a API **não** rejeita por falta de inscrição  
**And** o comportamento restante permanece o do stub de Avaliação (FR-7 fora deste módulo)  

---

### SPEC-6.3 — Porta de verificação disponível para o módulo Avaliação

**Given** que existem registros em `inscricao_aula`  
**When** um caso de uso consulta se `(estudanteId, aulaId)` está inscrito  
**Then** a porta de application/domain retorna verdadeiro/falso de forma determinística  
**And** essa porta é a mesma usada pelo gate do stub (AD-15)  

---

## NFR / regressão

### SPEC-NFR-I1 — Auth e catálogo intactos

**Given** o módulo 03 implementado  
**When** se executam os testes de auth (`AUTH_*`) e de catálogo (`CURSO_NOT_FOUND` / `AULA_NOT_FOUND`)  
**Then** continuam verdes sem mudança de contrato  

### SPEC-NFR-I2 — Envelope de erro

**Given** qualquer erro de domínio de inscrição (404/409/gate FR-6)  
**When** a API responde  
**Then** o corpo é `{ "code", "message", "traceId" }` (sem corpo Spring/Quarkus default)  
