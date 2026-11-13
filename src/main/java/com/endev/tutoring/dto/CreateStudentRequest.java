package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Aliya Nur") String name,
        @NotBlank @Email
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "aliya@example.com") String email) {
}
