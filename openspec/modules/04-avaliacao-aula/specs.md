# Specs — Módulo 04: Avaliação de Aula

| Campo | Valor |
| --- | --- |
| **Módulo** | `04-avaliacao-aula` |
| **FRs** | FR-7 (criar), FR-8 (listar), FR-9 (Urgência); prep. publish AD-4/AD-5 |
| **Formato** | Given / When / Then |
| **Status** | Rascunho para revisão |

> Specs normativas OpenSpec da change: `openspec/changes/avaliacao-aula-quarkus/specs/`.
> Este arquivo espelha o módulo para leitura humana (como 01–03).

---

## FR-7 — Criar Avaliação

### SPEC-7.1 — Criar com sucesso

**Given** Estudante inscrito na Aula e JWT `ESTUDANTE`  
**When** `POST /avaliacao` com `aulaId`, `descricao`, `nota` ∈ [0,10]  
**Then** **201** com `id`, `aulaId`, `cursoId`, `estudanteId` (=sub), `descricao`, `nota`, `urgencia`, `ocorridoEm`  
**And** Avaliação recuperável no GET  

### SPEC-7.2 — Nota inválida

**When** `nota` ∈ {-1, 11} ou ausente  
**Then** **400** `VALIDATION_ERROR` sem persistir  

### SPEC-7.3 — Descricao vazia

**When** `descricao` blank/ausente  
**Then** **400** `VALIDATION_ERROR` sem persistir  

### SPEC-7.4 — Aula inexistente

**When** `aulaId` não existe  
**Then** **404** `AULA_NOT_FOUND`  

### SPEC-7.5 — Duplicata

**Given** já existe Avaliação do mesmo Estudante na Aula  
**When** segundo POST com mesmo `aulaId`  
**Then** **409** `AVALIACAO_DUPLICADA`; original inalterada  

### SPEC-7.6 — Admin não cria

**When** JWT `ADMINISTRADOR` no POST  
**Then** **403** `AUTH_FORBIDDEN`  

### SPEC-7.7 — Sem token

**When** POST sem Authorization  
**Then** **401** `AUTH_MISSING_TOKEN` (módulo 01)  

---

## FR-6 (regressão) — Gate

### SPEC-6.1 — Sem inscrição

**When** Estudante não inscrito na Aula  
**Then** **403** `INSCRICAO_AULA_OBRIGATORIA`  

### SPEC-6.2 — Com inscrição

**When** Estudante inscrito + payload válido  
**Then** não rejeita por inscrição; segue FR-7  

---

## FR-8 — Listagem

### SPEC-8.1 — Campos

**When** Admin `GET /avaliacao`  
**Then** cada item tem `nota`, `urgencia`, `ocorridoEm`, `aulaId`, `cursoId`  

### SPEC-8.2 — Escopo Estudante

**Then** só Avaliações com `estudanteId` = `sub`  

### SPEC-8.3 — Escopo Admin

**Then** Avaliações de todos os Estudantes  

### SPEC-8.4 — Sem token

**Then** **401**  

---

## FR-9 — Urgência

| Nota | Urgência |
| --- | --- |
| ≤4 (ex.: 0, 4) | `ALTA` |
| 5–7 (ex.: 5, 7) | `MEDIA` |
| ≥8 (ex.: 8, 10) | `BAIXA` |

### SPEC-9.1–9.6 — Fronteiras

Cada fronteira 4 / 5 / 7 / 8 (e extremos 0 / 10) classifica conforme tabela.  

### SPEC-9.7 — Listagem inclui urgencia

GET devolve `urgencia`, `nota`, `ocorridoEm`.  

---

## Publish (prep. FR-10)

### SPEC-10prep.1 — ALTA publica

Após commit ALTA → `EvaluationEventPublisher` 1× com payload AD-5.  

### SPEC-10prep.2–3 — MÉDIA/BAIXA não publicam  

### SPEC-10prep.4 — Falha publish não apaga Avaliação; HTTP 201 permanece  

### SPEC-10prep.5 — JSON: `avaliacaoId`, `descricao`, `urgencia`, `ocorridoEm`, `aulaId`, `cursoId`  
