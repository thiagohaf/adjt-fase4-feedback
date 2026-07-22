package com.fiap.feedbacks.api.web.catalog;

import com.fiap.feedbacks.api.web.catalog.dto.AulaResponse;
import com.fiap.feedbacks.application.catalog.ConsultarAulaUseCase;
import com.fiap.feedbacks.domain.catalog.Aula;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/aulas")
@Produces(MediaType.APPLICATION_JSON)
public class AulaResource {

    @Inject
    ConsultarAulaUseCase consultarAulaUseCase;

    @GET
    @Path("/{id}")
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public AulaResponse consultar(@PathParam("id") UUID id) {
        return toAulaResponse(consultarAulaUseCase.execute(id));
    }

    private AulaResponse toAulaResponse(Aula aula) {
        return new AulaResponse(aula.id(), aula.cursoId(), aula.nome(), aula.descricao());
    }
}
