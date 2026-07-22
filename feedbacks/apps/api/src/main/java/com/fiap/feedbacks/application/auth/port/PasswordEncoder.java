package com.fiap.feedbacks.application.auth.port;

public interface PasswordEncoder {

    boolean matches(String rawPassword, String encodedPassword);

    String encode(String rawPassword);
}
