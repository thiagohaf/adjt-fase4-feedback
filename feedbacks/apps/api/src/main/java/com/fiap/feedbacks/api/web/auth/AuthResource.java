package com.fiap.feedbacks.api.web.auth;

import com.fiap.feedbacks.api.web.auth.dto.LoginRequest;
import com.fiap.feedbacks.api.web.auth.dto.LoginResponse;
import com.fiap.feedbacks.application.auth.LoginUseCase;
import com.fiap.feedbacks.application.auth.dto.LoginCommand;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    LoginUseCase loginUseCase;

    @POST
    @Path("/login")
    @PermitAll
    public LoginResponse login(@Valid LoginRequest request) {
        LOG.infof("Login attempt for email=%s", request.email());
        var result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
        return new LoginResponse(result.accessToken(), "Bearer", result.expiresInSeconds());
    }
}
