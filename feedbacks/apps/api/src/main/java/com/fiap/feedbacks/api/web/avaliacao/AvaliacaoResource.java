package com.fiap.feedbacks.api.web.avaliacao;

import com.fiap.feedbacks.application.avaliacao.ListarAvaliacoesUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GET: read model mínimo (SPEC-2.10/2.11). POST: stub provisório (módulos 02+).
 */
@Path("/api/v1/avaliacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvaliacaoResource {

    @Inject
    ListarAvaliacoesUseCase listarAvaliacoesUseCase;

    @GET
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<Map<String, Object>> listar() {
        return listarAvaliacoesUseCase.execute();
    }

    @POST
    @RolesAllowed("ESTUDANTE")
    public Response criar(Map<String, String> request) {
        String descricao = request == null ? "Feedback demo" : request.getOrDefault("descricao", "Feedback demo");
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        "id", UUID.randomUUID(),
                        "descricao", descricao
                ))
                .build();
    }
}
