# Como rodar a API no IntelliJ IDEA

Guia para iniciantes. Projeto: `feedbacks/apps/api` (Quarkus 3.33 LTS, Java 17).

## Pré-requisitos

1. **JDK 17** instalado (File → Project Structure → SDK = 17).
2. **Maven** (o IntelliJ já embute o Maven Wrapper se você abrir o `pom.xml`).
3. **PostgreSQL** local na porta `5432` (o profile `%local` **não** sobe o banco sozinho).

### Subir o Postgres (Docker)

No terminal:

```bash
docker run --name feedbacks-postgres \
  -e POSTGRES_USER=feedbacks \
  -e POSTGRES_PASSWORD=feedbacks \
  -e POSTGRES_DB=feedbacks \
  -p 5432:5432 \
  -d postgres:16
```

Credenciais esperadas pela API (`application.properties` profile `local`):

| Item | Valor |
| --- | --- |
| Host | `localhost:5432` |
| Database | `feedbacks` |
| User / senha | `feedbacks` / `feedbacks` |

Na primeira subida, o **Flyway** cria as tabelas e o seed de usuários demo.

---

## Abrir o projeto

1. IntelliJ → **Open**.
2. Selecione a pasta `feedbacks/apps/api` (a que contém o `pom.xml`)  
   **ou** abra a raiz do repositório e marque `feedbacks/apps/api` como módulo Maven.
3. Aguarde o IntelliJ indexar e baixar dependências (Maven sync).

### Plugin útil (opcional)

**Quarkus** (Plugins → Marketplace → “Quarkus”) — facilita Dev Mode e hot reload.

---

## Forma recomendada: Quarkus Dev Mode

### Opção A — Maven no IntelliJ

1. Abra a aba **Maven** (lado direito).
2. Expanda `feedbacks-api` → **Plugins** → **quarkus**.
3. Dê duplo clique em **`quarkus:dev`**.

### Opção B — Run Configuration

1. **Run** → **Edit Configurations…** → **+** → **Maven**.
2. Preencha:
   - **Name:** `quarkus:dev`
   - **Working directory:** `…/feedbacks/apps/api`
   - **Command line:** `quarkus:dev`
3. **Run** (botão verde).

Quando subir, você deve ver algo como:

```text
Listening on: http://localhost:8080
Profile local activated.
```

Dev Mode: altere código Java e a API recompila sozinha (sem reiniciar na maioria dos casos).

---

## Conferir se está no ar

No navegador ou Postman:

```http
GET http://localhost:8080/api/v1/health
```

Resposta esperada:

```json
{ "status": "UP" }
```

---

## Usuários de demo (login)

| Papel | Email | Senha |
| --- | --- | --- |
| ESTUDANTE | `estudante@demo.fiap` | `senha123` |
| ADMINISTRADOR | `admin@demo.fiap` | `admin123` |

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "estudante@demo.fiap",
  "password": "senha123"
}
```

Use o `accessToken` da resposta no header:

```http
Authorization: Bearer <accessToken>
```

---

## Postman

1. Importe `docs/postman/feedbacks-api.postman_collection.json`  
   (Postman → **Import** → selecione o arquivo).
2. Rode **Auth → Login (Estudante)** ou **Login (Admin)** — o token é salvo sozinho.
3. Depois chame as rotas protegidas (Cursos, Avaliações, etc.).

---

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
| --- | --- | --- |
| Falha ao conectar no datasource | Postgres parado / porta errada | Subir o container Docker acima |
| Porta 8080 em uso | Outro processo usando 8080 | Pare o outro processo ou use `-Dquarkus.http.port=8081` |
| Login 401 `AUTH_INVALID_CREDENTIALS` | Senha/email errados ou Flyway não rodou | Confira seed; veja logs do Flyway na subida |
| 401 em rota protegida sem `traceId` | API antiga / branch desatualizada | Atualize a branch e reinicie o `quarkus:dev` |

### Parar o Postgres Docker

```bash
docker stop feedbacks-postgres
# para remover o container:
docker rm feedbacks-postgres
```

---

## Alternativa: terminal (sem IntelliJ)

```bash
cd feedbacks/apps/api
mvn quarkus:dev
```
