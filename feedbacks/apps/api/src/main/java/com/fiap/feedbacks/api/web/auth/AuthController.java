package com.fiap.feedbacks.api.web.auth;

import com.fiap.feedbacks.api.web.auth.dto.LoginRequest;
import com.fiap.feedbacks.api.web.auth.dto.LoginResponse;
import com.fiap.feedbacks.application.auth.LoginUseCase;
import com.fiap.feedbacks.application.auth.dto.LoginCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.expiresInSeconds()
        ));
    }
}
