package com.fiap.feedbacks.application.auth.port;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;

public interface TokenService {

    String generate(AuthenticatedUser user);

    AuthenticatedUser parse(String token);

    long expiresInSeconds();
}
