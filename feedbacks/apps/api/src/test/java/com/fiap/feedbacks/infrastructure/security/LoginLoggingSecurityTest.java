package com.fiap.feedbacks.infrastructure.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fiap.feedbacks.support.TestDataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataInitializer.class)
class LoginLoggingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SPEC-NFR-2 — Senha nunca em log")
    void specNfr2_passwordNotLogged() throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);

        var success = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"estudante@demo.fiap","password":"senha123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"estudante@demo.fiap","password":"errada"}
                                """))
                .andExpect(status().isUnauthorized());

        var token = success.getResponse().getContentAsString();
        var logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

        assertThat(logs).noneMatch(log -> log.contains("senha123") || log.contains("errada"));
        assertThat(String.join(" ", logs)).doesNotContain(token.substring(0, Math.min(token.length(), 20)));
    }
}
