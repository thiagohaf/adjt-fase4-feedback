package com.fiap.feedbacks.api.web.health;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * Health público em /api/v1/health (AD-9). SmallRye Health permanece em /q/health.
 */
@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    @PermitAll
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
