package com.fiap.feedbacks.avaliacao;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.infrastructure.messaging.FakeEvaluationEventPublisher;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AvaliacaoCriarQuarkusTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Inject
    FakeEvaluationEventPublisher fakePublisher;

    @BeforeEach
    void clearPublisher() {
        fakePublisher.clear();
    }

    @Test
    @DisplayName("SPEC-7.1 — Criar Avaliação com sucesso")
    void spec7_1_criarComSucesso() {
        Context ctx = prepareInscrito("Curso 7.1");

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), "Aula rápida demais", 3))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("aulaId", equalTo(ctx.aulaId()))
                .body("cursoId", equalTo(ctx.cursoId()))
                .body("estudanteId", equalTo(ESTUDANTE_ID.toString()))
                .body("descricao", equalTo("Aula rápida demais"))
                .body("nota", equalTo(3))
                .body("urgencia", equalTo("ALTA"))
                .body("ocorridoEm", notNullValue());

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .when()
                .get("/api/v1/avaliacoes")
                .then()
                .statusCode(200)
                .body("find { it.aulaId == '%s' }.nota".formatted(ctx.aulaId()), equalTo(3));
    }

    @Test
    @DisplayName("SPEC-7.2 — Nota inválida retorna 400 VALIDATION_ERROR")
    void spec7_2_notaInvalida() {
        Context ctx = prepareInscrito("Curso 7.2");

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), "Feedback", 11))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-7.3 — Descricao vazia retorna 400 VALIDATION_ERROR")
    void spec7_3_descricaoVazia() {
        Context ctx = prepareInscrito("Curso 7.3");

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body("{\"aulaId\":\"%s\",\"descricao\":\"   \",\"nota\":5}".formatted(ctx.aulaId()))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-7.4 — Aula inexistente")
    void spec7_4_aulaInexistente() {
        String token = loginEstudante();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body(UUID.randomUUID().toString(), "Feedback", 5))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(404)
                .body("code", equalTo("AULA_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-7.5 — Segunda Avaliação da mesma Aula é conflito")
    void spec7_5_duplicata() {
        Context ctx = prepareInscrito("Curso 7.5");

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), "Primeira", 5))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), "Segunda", 6))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(409)
                .body("code", equalTo("AVALIACAO_DUPLICADA"));
    }

    @Test
    @DisplayName("SPEC-7.6 — Admin recebe AUTH_FORBIDDEN")
    void spec7_6_adminForbidden() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body(UUID.randomUUID().toString(), "Feedback", 5))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-7.7 — Sem token")
    void spec7_7_semToken() {
        given()
                .contentType(ContentType.JSON)
                .body(body(UUID.randomUUID().toString(), "Feedback", 5))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("SPEC-9.1/9.3/9.5 — Fronteiras Urgência 4/5/8")
    void spec9_fronteirasUrgencia() {
        assertUrgencia(4, "ALTA");
        assertUrgencia(5, "MEDIA");
        assertUrgencia(8, "BAIXA");
    }

    @Test
    @DisplayName("SPEC-8.1 — Admin lista com campos de domínio")
    void spec8_1_adminListaEnriquecida() {
        Context ctx = prepareInscrito("Curso 8.1");
        createAvaliacao(ctx, "Feedback lista", 4);

        given()
                .header("Authorization", "Bearer " + login("admin@demo.fiap", "admin123"))
                .when()
                .get("/api/v1/avaliacoes")
                .then()
                .statusCode(200)
                .body("find { it.aulaId == '%s' }".formatted(ctx.aulaId()), hasKey("nota"))
                .body("find { it.aulaId == '%s' }.urgencia".formatted(ctx.aulaId()), equalTo("ALTA"))
                .body("find { it.aulaId == '%s' }.cursoId".formatted(ctx.aulaId()), equalTo(ctx.cursoId()))
                .body("find { it.aulaId == '%s' }.ocorridoEm".formatted(ctx.aulaId()), notNullValue());
    }

    @Test
    @DisplayName("SPEC-8.2 — Estudante não vê Avaliações alheias")
    void spec8_2_estudanteNaoVeAlheias() {
        Context a = prepareInscrito("Curso 8.2 A");
        createAvaliacao(a, "De A", 5);

        String adminToken = login("admin@demo.fiap", "admin123");
        String estudante2 = login("estudante2@demo.fiap", "senha123");
        String cursoId = createCurso(adminToken, "Curso 8.2 B " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula B");
        enroll(estudante2, cursoId, aulaId);
        given()
                .header("Authorization", "Bearer " + estudante2)
                .contentType(ContentType.JSON)
                .body(body(aulaId, "De B", 6))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201);

        var proprias = given()
                .header("Authorization", "Bearer " + a.estudanteToken())
                .when()
                .get("/api/v1/avaliacoes")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("estudanteId", String.class);

        assertThat(proprias).isNotEmpty().containsOnly(ESTUDANTE_ID.toString());
    }

    @Test
    @DisplayName("SPEC-10prep.1/2/3/5 — Publish só ALTA com payload AD-5")
    void spec10prep_publishAlta() {
        Context alta = prepareInscrito("Curso 10 ALTA");
        createAvaliacao(alta, "Alerta", 3);

        assertThat(fakePublisher.publishedEvents()).hasSize(1);
        AvaliacaoAlertaEvent event = fakePublisher.publishedEvents().get(0);
        assertThat(event.urgencia().name()).isEqualTo("ALTA");
        assertThat(event.avaliacaoId()).isNotNull();
        assertThat(event.descricao()).isEqualTo("Alerta");
        assertThat(event.aulaId().toString()).isEqualTo(alta.aulaId());
        assertThat(event.cursoId().toString()).isEqualTo(alta.cursoId());
        assertThat(event.ocorridoEm()).isNotNull();

        fakePublisher.clear();
        Context media = prepareInscrito("Curso 10 MEDIA");
        createAvaliacao(media, "Media", 6);
        assertThat(fakePublisher.publishedEvents()).isEmpty();

        Context baixa = prepareInscrito("Curso 10 BAIXA");
        createAvaliacao(baixa, "Baixa", 9);
        assertThat(fakePublisher.publishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("SPEC-10prep.4 — Publish falha e Avaliação permanece")
    void spec10prep_falhaPublishMantem() {
        Context ctx = prepareInscrito("Curso 10 fail");
        fakePublisher.failNextPublish();

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), "Mesmo com falha", 2))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201)
                .body("urgencia", equalTo("ALTA"));

        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .when()
                .get("/api/v1/avaliacoes")
                .then()
                .statusCode(200)
                .body("find { it.aulaId == '%s' }.descricao".formatted(ctx.aulaId()),
                        equalTo("Mesmo com falha"));
    }

    private void assertUrgencia(int nota, String urgencia) {
        Context ctx = prepareInscrito("Curso urg " + nota + " " + UUID.randomUUID());
        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), "Nota " + nota, nota))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201)
                .body("urgencia", equalTo(urgencia))
                .body("nota", equalTo(nota));
    }

    private void createAvaliacao(Context ctx, String descricao, int nota) {
        given()
                .header("Authorization", "Bearer " + ctx.estudanteToken())
                .contentType(ContentType.JSON)
                .body(body(ctx.aulaId(), descricao, nota))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201);
    }

    private Context prepareInscrito(String cursoNome) {
        String adminToken = login("admin@demo.fiap", "admin123");
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, cursoNome + " " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula");
        enroll(estudanteToken, cursoId, aulaId);
        return new Context(estudanteToken, cursoId, aulaId);
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

    private String loginEstudante() {
        return login("estudante@demo.fiap", "senha123");
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

    private static String body(String aulaId, String descricao, int nota) {
        return "{\"aulaId\":\"%s\",\"descricao\":\"%s\",\"nota\":%d}".formatted(aulaId, descricao, nota);
    }

    private record Context(String estudanteToken, String cursoId, String aulaId) {
    }
}
