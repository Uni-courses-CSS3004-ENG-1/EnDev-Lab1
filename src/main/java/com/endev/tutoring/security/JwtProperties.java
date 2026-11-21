package com.endev.tutoring.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// app.jwt from yaml. The secret comes from application-dev.yaml on the laptop and from
// CSS_JWT_SECRET in prod; Java never holds it. A short value such as "secret" stops the boot.
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
        @NotBlank String issuer) {

    // the record's default toString would print the secret into a log line
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", secret=****]";
    }
}
