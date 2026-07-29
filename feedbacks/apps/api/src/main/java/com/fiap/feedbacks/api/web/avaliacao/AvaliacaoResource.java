package com.fiap.feedbacks.api.web.avaliacao;

import com.fiap.feedbacks.api.web.avaliacao.dto.AvaliacaoResponse;
import com.fiap.feedbacks.api.web.avaliacao.dto.CriarAvaliacaoRequest;
import com.fiap.feedbacks.application.avaliacao.CriarAvaliacaoUseCase;
import com.fiap.feedbacks.application.avaliacao.ListarAvaliacoesUseCase;
import com.fiap.feedbacks.domain.avaliacao.Avaliacao;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * GET: listagem enriquecida (FR-8 / SPEC-2.10–2.11).
 * POST: criação real de Avaliação (FR-7/FR-9) com gate FR-6.
 */
@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvaliacaoResource {

    @Inject
    ListarAvaliacoesUseCase listarAvaliacoesUseCase;
    @Inject
    CriarAvaliacaoUseCase criarAvaliacaoUseCase;

    @GET
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<Map<String, Object>> listar() {
        return listarAvaliacoesUseCase.execute();
    }

    @POST
    @RolesAllowed("ESTUDANTE")
    public Response criar(@Valid CriarAvaliacaoRequest request) {
        Avaliacao avaliacao = criarAvaliacaoUseCase.execute(
                request.aulaId(),
                request.descricao(),
                request.nota()
        );
        return Response.status(Response.Status.CREATED)
                .entity(AvaliacaoResponse.from(avaliacao))
                .build();
    }
}
