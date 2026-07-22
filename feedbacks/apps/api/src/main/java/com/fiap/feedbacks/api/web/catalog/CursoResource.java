package com.fiap.feedbacks.api.web.catalog;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stub provisório — módulos 02+ substituem o corpo; path e @RolesAllowed são o contrato estável.
 */
@Path("/api/v1/cursos")
@Produces(MediaType.APPLICATION_JSON)
public class CursoResource {

    @GET
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<Map<String, Object>> listar() {
        return List.of(Map.of("id", UUID.randomUUID(), "nome", "Curso Demo"));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response criar(Map<String, String> request) {
        String nome = request == null ? "Novo Curso" : request.getOrDefault("nome", "Novo Curso");
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("id", UUID.randomUUID(), "nome", nome))
                .build();
    }

    @POST
    @Path("/{cursoId}/aulas")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response criarAula(@PathParam("cursoId") UUID cursoId, Map<String, String> request) {
        String titulo = request == null ? "Nova Aula" : request.getOrDefault("titulo", "Nova Aula");
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        "id", UUID.randomUUID(),
                        "cursoId", cursoId,
                        "titulo", titulo
                ))
                .build();
    }

    @GET
    @Path("/{cursoId}/aulas")
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<Map<String, Object>> listarAulas(@PathParam("cursoId") UUID cursoId) {
        return List.of(Map.of(
                "id", UUID.randomUUID(),
                "cursoId", cursoId,
                "titulo", "Aula Demo"
        ));
    }

    @POST
    @Path("/{cursoId}/inscricoes")
    @RolesAllowed("ESTUDANTE")
    public Response inscrever(@PathParam("cursoId") UUID cursoId) {
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("cursoId", cursoId, "status", "INSCRITO"))
                .build();
    }
}
