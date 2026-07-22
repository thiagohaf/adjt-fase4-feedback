package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.port.PasswordEncoder;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BcryptPasswordEncoder implements PasswordEncoder {

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return BcryptUtil.matches(rawPassword, encodedPassword);
    }

    @Override
    public String encode(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }
}
