package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// The only body in this API that carries a password.
public record LoginRequest(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Email (or an older username)",
                example = "aliya@example.com") String username,
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "student-dev",
                format = "password") String password) {

    // the record's default toString would put the password into any log line that prints it
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}
