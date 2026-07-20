package com.fiap.feedbacks.auth;

import com.fiap.feedbacks.infrastructure.persistence.UsuarioEntity;
import com.fiap.feedbacks.infrastructure.persistence.UsuarioJpaRepository;
import com.fiap.feedbacks.support.TestDataInitializer;
import com.fiap.feedbacks.support.JwtClaimsReader;
import com.fiap.feedbacks.support.JwtTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataInitializer.class)
class AuthIntegrationTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Nested
    @DisplayName("FR-1 — Login e emissão de JWT")
    class Fr1Login {

        @Test
        @DisplayName("SPEC-1.1 — Login bem-sucedido (Estudante)")
        void spec11_loginEstudante() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"estudante@demo.fiap","password":"senha123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber())
                    .andReturn();

            var token = readJsonField(result, "accessToken");
            var claims = JwtClaimsReader.readClaims(token, jwtSecret);

            assertThat(claims.getSubject()).isEqualTo(ESTUDANTE_ID.toString());
            assertThat(claims.get("role", String.class)).isEqualTo("ESTUDANTE");
            assertThat(claims.getExpiration().toInstant()).isAfter(java.time.Instant.now());
        }

        @Test
        @DisplayName("SPEC-1.2 — Login bem-sucedido (Administrador)")
        void spec12_loginAdmin() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"admin@demo.fiap","password":"admin123"}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn();

            var token = readJsonField(result, "accessToken");
            var claims = JwtClaimsReader.readClaims(token, jwtSecret);
            assertThat(claims.get("role", String.class)).isEqualTo("ADMINISTRADOR");
        }

        @Test
        @DisplayName("SPEC-1.3 — Credenciais inválidas (email inexistente)")
        void spec13_emailInexistente() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"naoexiste@demo.fiap","password":"qualquer"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.traceId").exists());
        }

        @Test
        @DisplayName("SPEC-1.4 — Credenciais inválidas (senha incorreta)")
        void spec14_senhaIncorreta() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"estudante@demo.fiap","password":"errada"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("SPEC-1.7 — Acesso a rota protegida com token válido")
        void spec17_rotaProtegidaComToken() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(get("/api/v1/cursos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SPEC-1.8 — Acesso a rota protegida sem token")
        void spec18_semToken() throws Exception {
            mockMvc.perform(get("/api/v1/cursos"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_MISSING_TOKEN"));
        }

        @Test
        @DisplayName("SPEC-1.9 — Acesso a rota protegida com token malformado")
        void spec19_tokenMalformado() throws Exception {
            mockMvc.perform(get("/api/v1/cursos")
                            .header("Authorization", "Bearer token-invalido"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
        }

        @Test
        @DisplayName("SPEC-1.10 — Acesso a rota protegida com token expirado")
        void spec110_tokenExpirado() throws Exception {
            var expiredToken = JwtTestHelper.createExpiredToken(jwtSecret, ESTUDANTE_ID, "ESTUDANTE");

            mockMvc.perform(get("/api/v1/cursos")
                            .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
        }

        @Test
        @DisplayName("SPEC-1.11 — Rotas públicas sem autenticação")
        void spec111_rotasPublicas() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"estudante@demo.fiap","password":"errada"}
                                    """))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("FR-2 — Autorização por papel")
    class Fr2Autorizacao {

        @Test
        @DisplayName("SPEC-2.1 — Estudante não cria Curso")
        void spec21_estudanteNaoCriaCurso() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(post("/api/v1/cursos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Novo Curso\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("SPEC-2.2 — Estudante não cria Aula")
        void spec22_estudanteNaoCriaAula() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\":\"Nova Aula\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("SPEC-2.3 — Administrador cria Curso e Aula")
        void spec23_adminCriaCursoEAula() throws Exception {
            var token = login("admin@demo.fiap", "admin123");

            mockMvc.perform(post("/api/v1/cursos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Novo Curso\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\":\"Nova Aula\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("SPEC-2.4 — Administrador consulta catálogo")
        void spec24_adminConsultaCatalogo() throws Exception {
            var token = login("admin@demo.fiap", "admin123");
            var cursoId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/cursos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/cursos/" + cursoId + "/aulas")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SPEC-2.5 — Estudante consulta catálogo")
        void spec25_estudanteConsultaCatalogo() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(get("/api/v1/cursos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/aulas/" + UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SPEC-2.6 — Criação de Avaliação exige papel Estudante")
        void spec26_adminNaoCriaAvaliacao() throws Exception {
            var token = login("admin@demo.fiap", "admin123");

            mockMvc.perform(post("/api/v1/avaliacoes")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descricao\":\"Feedback\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("SPEC-2.7 — Estudante cria Avaliação (autorização OK)")
        void spec27_estudanteCriaAvaliacao() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(post("/api/v1/avaliacoes")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descricao\":\"Feedback\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("SPEC-2.8 — Inscrição exige papel Estudante")
        void spec28_adminNaoInscreve() throws Exception {
            var token = login("admin@demo.fiap", "admin123");

            mockMvc.perform(post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("SPEC-2.9 — Estudante realiza inscrição")
        void spec29_estudanteInscreve() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("SPEC-2.10 — Listagem de Avaliações: Admin vê todas")
        void spec210_adminVeTodasAvaliacoes() throws Exception {
            var token = login("admin@demo.fiap", "admin123");

            mockMvc.perform(get("/api/v1/avaliacoes")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("SPEC-2.11 — Listagem de Avaliações: Estudante vê só as próprias")
        void spec211_estudanteVeApenasPropriasAvaliacoes() throws Exception {
            var token = login("estudante@demo.fiap", "senha123");

            mockMvc.perform(get("/api/v1/avaliacoes")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].estudanteId").value(ESTUDANTE_ID.toString()));
        }
    }

    @Nested
    @DisplayName("Requisitos não-funcionais")
    class Nfr {

        @Test
        @DisplayName("SPEC-NFR-3 — Senha armazenada com hash")
        void specNfr3_senhaComHash() {
            UsuarioEntity usuario = usuarioJpaRepository.findByEmail("estudante@demo.fiap").orElseThrow();

            assertThat(usuario.getSenhaHash()).startsWith("$2a$");
            assertThat(passwordEncoder.matches("senha123", usuario.getSenhaHash())).isTrue();
            assertThat(usuario.getSenhaHash()).isNotEqualTo("senha123");
        }
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJsonField(result, "accessToken");
    }

    private String readJsonField(MvcResult result, String field) throws Exception {
        var node = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        return node.get(field).asText();
    }
}
