package com.fiap.feedbacks.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class AuthRoleAuthorizationTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Test
    @DisplayName("SPEC-2.1 — Estudante não cria Curso")
    void spec2_1_estudanteNaoCriaCurso() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Novo Curso\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-2.2 — Estudante não cria Aula")
    void spec2_2_estudanteNaoCriaAula() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Nova Aula\"}")
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-2.3 — Administrador cria Curso e Aula")
    void spec2_3_adminCriaCursoEAula() {
        String token = login("admin@demo.fiap", "admin123");

        String cursoId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Novo Curso\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Nova Aula\"}")
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("SPEC-2.4 — Administrador consulta catálogo")
    void spec2_4_adminConsultaCatalogo() {
        String token = login("admin@demo.fiap", "admin123");
        String cursoId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Curso Auth Consulta\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("SPEC-2.5 — Estudante consulta catálogo")
    void spec2_5_estudanteConsultaCatalogo() {
        String adminToken = login("admin@demo.fiap", "admin123");
        String cursoId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Curso Auth Estudante\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .extract().path("id");
        String aulaId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Aula Auth Estudante\"}")
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(201)
                .extract().path("id");

        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/aulas/" + aulaId)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("SPEC-2.6 — Admin não cria Avaliação")
    void spec2_6_adminNaoCriaAvaliacao() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"Feedback\"}")
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-2.7 — Estudante cria Avaliação")
    void spec2_7_estudanteCriaAvaliacao() {
        String adminToken = login("admin@demo.fiap", "admin123");
        String estudanteToken = login("estudante@demo.fiap", "senha123");
        String cursoId = createCurso(adminToken, "Curso Auth Avaliacao " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula Auth Avaliacao");

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas/" + aulaId + "/inscricoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .contentType(ContentType.JSON)
                .body("{\"aulaId\":\"%s\",\"descricao\":\"Feedback\",\"nota\":7}".formatted(aulaId))
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(201)
                .body("urgencia", equalTo("MEDIA"))
                .body("nota", equalTo(7));
    }

    @Test
    @DisplayName("SPEC-2.8 — Inscrição exige papel Estudante")
    void spec2_8_adminNaoInscreve() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("SPEC-2.9 — Estudante realiza inscrição")
    void spec2_9_estudanteInscreve() {
        String adminToken = login("admin@demo.fiap", "admin123");
        String estudanteToken = login("estudante@demo.fiap", "senha123");
        String cursoId = createCurso(adminToken, "Curso Auth Inscricao " + UUID.randomUUID());

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("SPEC-2.10 — Admin vê todas Avaliações")
    void spec2_10_adminVeTodas() {
        String adminToken = login("admin@demo.fiap", "admin123");
        String estudanteToken = login("estudante@demo.fiap", "senha123");
        String estudante2Token = login("estudante2@demo.fiap", "senha123");

        String cursoA = createCurso(adminToken, "Curso Auth List A " + UUID.randomUUID());
        String aulaA = createAula(adminToken, cursoA, "Aula A");
        enroll(estudanteToken, cursoA, aulaA);
        createAvaliacao(estudanteToken, aulaA, "De A", 5);

        String cursoB = createCurso(adminToken, "Curso Auth List B " + UUID.randomUUID());
        String aulaB = createAula(adminToken, cursoB, "Aula B");
        enroll(estudante2Token, cursoB, aulaB);
        createAvaliacao(estudante2Token, aulaB, "De B", 8);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/avaliacao")
                .then()
                .statusCode(200)
                .body("findAll { it.aulaId == '%s' || it.aulaId == '%s' }".formatted(aulaA, aulaB), hasSize(2));
    }

    @Test
    @DisplayName("SPEC-2.11 — Estudante vê só as próprias Avaliações")
    void spec2_11_estudanteVeProprias() {
        String adminToken = login("admin@demo.fiap", "admin123");
        String estudanteToken = login("estudante@demo.fiap", "senha123");
        String estudante2Token = login("estudante2@demo.fiap", "senha123");

        String cursoA = createCurso(adminToken, "Curso Auth Own A " + UUID.randomUUID());
        String aulaA = createAula(adminToken, cursoA, "Aula Own A");
        enroll(estudanteToken, cursoA, aulaA);
        createAvaliacao(estudanteToken, aulaA, "Própria", 6);

        String cursoB = createCurso(adminToken, "Curso Auth Own B " + UUID.randomUUID());
        String aulaB = createAula(adminToken, cursoB, "Aula Own B");
        enroll(estudante2Token, cursoB, aulaB);
        createAvaliacao(estudante2Token, aulaB, "Alheia", 4);

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .get("/avaliacao")
                .then()
                .statusCode(200)
                .body("estudanteId", org.hamcrest.Matchers.everyItem(equalTo(ESTUDANTE_ID.toString())))
                .body("find { it.aulaId == '%s' }.descricao".formatted(aulaA), equalTo("Própria"));
    }

    private void enroll(String estudanteToken, String cursoId, String aulaId) {
        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201);
        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas/" + aulaId + "/inscricoes")
                .then()
                .statusCode(201);
    }

    private void createAvaliacao(String estudanteToken, String aulaId, String descricao, int nota) {
        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .contentType(ContentType.JSON)
                .body("{\"aulaId\":\"%s\",\"descricao\":\"%s\",\"nota\":%d}".formatted(aulaId, descricao, nota))
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(201);
    }

    private String createCurso(String token, String nome) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"%s\"}".formatted(nome))
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createAula(String token, String cursoId, String nome) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"%s\"}".formatted(nome))
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String login(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("accessToken");
    }
}
