package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTutorRequest(
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Dana Smith") String name,
        @NotBlank @Email
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "dana@example.com") String email,
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Algebra") String subject,
        @NotNull @Positive @Digits(integer = 8, fraction = 2)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "25.00") BigDecimal hourlyRate) {
}
