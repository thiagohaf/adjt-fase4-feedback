package com.fiap.feedbacks.api.web.catalog;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.UUID;

/**
 * Stub provisório — módulos 02+ substituem o corpo; path e @RolesAllowed são o contrato estável.
 */
@Path("/api/v1/aulas")
@Produces(MediaType.APPLICATION_JSON)
public class AulaResource {

    @GET
    @Path("/{id}")
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public Map<String, Object> consultar(@PathParam("id") UUID id) {
        return Map.of("id", id, "titulo", "Aula Demo");
    }
}
