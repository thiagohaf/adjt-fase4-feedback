# Como rodar a API no IntelliJ IDEA

Guia para iniciantes. Projeto: `feedbacks/apps/api` (Quarkus **3.33.2.1** LTS, Java 17).

## Pré-requisitos

1. **JDK 17** instalado (File → Project Structure → SDK = 17).
2. **Maven** — o IntelliJ embute o Maven; não há `mvnw` neste módulo.
3. **PostgreSQL** local na porta `5432` (o profile `%local` **não** sobe o banco sozinho; Dev Services só no profile `%test`).

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

Se o container já existir: `docker start feedbacks-postgres`.

Credenciais esperadas pela API (`application.properties` profile `local`):

| Item | Valor |
| --- | --- |
| Host | `localhost:5432` |
| Database | `feedbacks` |
| User / senha | `feedbacks` / `feedbacks` |

Na primeira subida, o **Flyway** (V1–V5) cria as tabelas e o seed de usuários demo.

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
{ "status": "UP", "database": "UP" }
```

Readiness (mesmo probe do ALB na AWS):

```http
GET http://localhost:8080/q/health/ready
```

---

## Usuários de demo (login)

Seed em `V2__seed_usuarios.sql`:

| Papel | Email | Senha |
| --- | --- | --- |
| ESTUDANTE | `estudante@demo.fiap` | `senha123` |
| ADMINISTRADOR | `admin@demo.fiap` | `admin123` |
| ESTUDANTE | `estudante2@demo.fiap` | `senha123` |

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

1. Importe `docs/postman/feedbacks-api.postman_collection.json` e o environment  
   `feedbacks-api.local.postman_environment.json` (Postman → **Import**).
2. Selecione o environment **Feedbacks API — Local**.
3. Rode **Auth → Login (Estudante)** ou **Login (Admin)** — o token é salvo sozinho em `accessToken`.
4. Depois chame as rotas protegidas (Cursos, Aulas, Avaliações, etc.).

Para a demo no ALB, importe também `feedbacks-api.aws-alb.postman_environment.json`
e selecione **Feedbacks API — AWS ALB (demo)** (atualize o `baseUrl` com o DNS do último `deploy(all)`; ver `docs/ROTEIRO-DEMO.md`).

---

## Rodar os testes no IntelliJ

Na aba **Maven** → `feedbacks-api` → **Lifecycle** → **`verify`**  
(ou terminal: `mvn -B verify` em `feedbacks/apps/api`).

Isso executa unitários + `@QuarkusTest` (Dev Services sobe um Postgres de teste) e o gate **JaCoCo ≥ 90%** de linha. Relatório HTML: `target/jacoco-report/`.

---

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
| --- | --- | --- |
| Falha ao conectar no datasource | Postgres parado / porta errada | `docker start feedbacks-postgres` ou subir o container acima |
| `The container name is already in use` | Container antigo parado | `docker start feedbacks-postgres` |
| Porta 8080 em uso | Outro processo usando 8080 | Pare o outro processo ou use `-Dquarkus.http.port=8081` |
| Login 401 `AUTH_INVALID_CREDENTIALS` | Senha/email errados ou Flyway não rodou | Confira seed; veja logs do Flyway na subida |
| 401 em rota protegida sem `traceId` | API antiga / branch desatualizada | Atualize a branch e reinicie o `quarkus:dev` |
| `mvn verify` falha no JaCoCo | Cobertura &lt; 90% | Corrija testes; report em `target/jacoco-report/` |

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
