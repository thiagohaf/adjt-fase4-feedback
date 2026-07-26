package com.fiap.feedbacks.api.web.health;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health público em {@code /api/v1/health} (AD-9 / FR-13).
 * Reflete conectividade básica com o PostgreSQL; SmallRye permanece em {@code /q/health}.
 */
@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class HealthResource {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    @Inject
    DataSource dataSource;

    @GET
    @PermitAll
    public Response health() {
        Map<String, Object> body = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                body.put("status", "DOWN");
                body.put("database", "invalid");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(body).build();
            }
            body.put("status", "UP");
            body.put("database", "UP");
            return Response.ok(body).build();
        } catch (SQLException ex) {
            body.put("status", "DOWN");
            body.put("database", "DOWN");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(body).build();
        }
    }
}
