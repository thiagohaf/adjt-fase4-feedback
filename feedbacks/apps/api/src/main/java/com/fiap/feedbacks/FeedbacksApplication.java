package com.fiap.feedbacks;

import com.fiap.feedbacks.infrastructure.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
        org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class
})
@EnableConfigurationProperties(JwtProperties.class)
public class FeedbacksApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbacksApplication.class, args);
    }
}
